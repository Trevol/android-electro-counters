package com.tavrida.counter_scanner.scanning

import android.graphics.Bitmap
import android.graphics.RectF
import com.tavrida.counter_scanner.aggregation.AggregatedDetections
import com.tavrida.counter_scanner.aggregation.AggregatingBoxGroupingDigitExtractor
import com.tavrida.counter_scanner.aggregation.DigitAtLocation
import com.tavrida.counter_scanner.detection.ScreenDigitDetector
import com.tavrida.counter_scanner.utils.rgb2gray
import com.tavrida.electro_counters.tracking.AggregatedDigitDetectionTracker
import com.tavrida.utils.log
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.Closeable
import java.util.*
import kotlin.IllegalStateException
import kotlin.collections.ArrayList

class NonblockingCounterReadingScanner(
    detector: ScreenDigitDetector,
    val detectorRoi: DetectionRoi,
    val readingStabilityThresholdMs: Long
) : Closeable {
    data class ScanResult(
        val digitsAtLocations: List<DigitAtLocation>,
        val aggregatedDetections: List<AggregatedDetections>,
        val readingInfo: ReadingInfo?
    ) {
        data class ReadingInfo(val reading: String, val millisecondsOfStability: Long)
    }

    var stopped = false
    private val bitmapToMats = BitmapToMats()
    private val detectionTracker = AggregatedDigitDetectionTracker()
    private val digitExtractor = AggregatingBoxGroupingDigitExtractor()
    private val detectorJob = DetectorJob(detector, detectionTracker, digitExtractor)

    private var serialSeq = 0
    private val prevFrameItems = mutableListOf<SerialGrayItem>()
    private var actualDetections = listOf<AggregatedDetections>()

    private fun ensureStarted() {
        if (stopped) throw IllegalStateException("Scanner is stopped")
    }

    override fun close() = stop()
    fun stop() {
        ensureStarted()
        detectorJob.stop()
        prevFrameItems.clear()
        stopped = true
    }

    private fun scanQR(rgbMat: Mat) {
        //TODO()
    }

    fun scan(inputImg: Bitmap): ScanResult {
        ensureStarted()

        val (detectionRoiImg, _, roiOrigin) = detectorRoi.extractImage(inputImg)
        val (rgbMat, grayMat) = bitmapToMats.convert(inputImg)

        scanQR(rgbMat)

        detectorJob.input.put(DetectorJobInputItem(serialSeq, detectionRoiImg, roiOrigin, grayMat))
        if (prevFrameItems.isEmpty()) {
            "prevFrameItems.isEmpty()".log("NonblockingCounterReadingScanner")
            // special processing of first frame
            // no prev frame and detections to continue processing - so skipping processing
            prevFrameItems.add(SerialGrayItem(serialSeq, grayMat))
            callId++
            return noDetections()
        }
        // TODO: specify timeout here - scanner blocked for infinite time if detector job stopped
        //      unexpectedly. Or handle exceptions in detector job
        val detectorResult = detectorJob.output.keepLastOrNull()
        if (detectorResult != null) {
            val frames = prevFrameItems.bySerialId(detectorResult.serialId)
                .map { it.gray }
                .toMutableList()
            "callId $callId. detectorResult != null. serialId: ${detectorResult.serialId}. prevFrameItems(${prevFrameItems.map { it.serialId }}) frames(${frames.size}) "
                .log("NonblockingCounterReadingScanner")
            frames.add(grayMat)
            actualDetections = detectionTracker.track(frames, detectorResult.detections)
            prevFrameItems.clear()
        } else {
            "callId $callId. detectorResult == null. prevFrameItems(${prevFrameItems.map { it.serialId }}"
                .log("NonblockingCounterReadingScanner")
            val prevGray = prevFrameItems.last().gray
            actualDetections = detectionTracker.track(prevGray, grayMat, actualDetections)
        }

        prevFrameItems.add(SerialGrayItem(serialSeq, grayMat))
        "callId $callId. prevFrameItems.add. prevFrameItems(${prevFrameItems.map { it.serialId }})"
            .log("NonblockingCounterReadingScanner")
        serialSeq++

        val digitsAtBoxes = digitExtractor.extractDigits(actualDetections)
        val readingInfo = readingInfo(digitsAtBoxes)
        callId++
        return ScanResult(digitsAtBoxes, actualDetections, readingInfo)
    }

    var prevReading = ""
    var startOfReadingMs = -1L
    private fun readingInfo(digitsAtLocations: List<DigitAtLocation>): ScanResult.ReadingInfo? {
        if (digitsAtLocations.isEmpty())
            return null
        val reading = digitsAtLocations.toReading()
        if (reading != prevReading) { // new reading available
            prevReading = reading
            startOfReadingMs = System.currentTimeMillis()
        }

        val msOfReadingStability = System.currentTimeMillis() - startOfReadingMs
        if (msOfReadingStability >= readingStabilityThresholdMs)
            return ScanResult.ReadingInfo(reading, msOfReadingStability)
        return null

    }

    private companion object {
        var callId = 0
        private fun List<DigitAtLocation>.toReading(): String {
            return sortedBy { it.location.left }
                .removeVerticalDigits()
                .map { it.digit }
                .joinToString("") { it.toString() }
        }

        private fun List<DigitAtLocation>.removeVerticalDigits(): List<DigitAtLocation> {
            if (size <= 1)
                return this
            val resultItems = ArrayList<DigitAtLocation>(size)
            val srcItems = LinkedList(this)

            while (srcItems.isNotEmpty()) {
                val thisItem = srcItems.first()
                val restOfItems = srcItems.subList(1, srcItems.size)
                val verticalItem = thisItem.getVertical(restOfItems)
                if (verticalItem == null) {
                    resultItems.add(thisItem)
                } else {
                    resultItems.add(chooseVerticallyLower(thisItem, verticalItem))
                    srcItems.remove(verticalItem)
                }
                srcItems.remove(thisItem)
            }

            return resultItems
        }

        private fun chooseVerticallyLower(d1: DigitAtLocation, d2: DigitAtLocation) =
            if (d1.location.top > d2.location.top) d1 else d2

        private fun DigitAtLocation.getVertical(others: Iterable<DigitAtLocation>): DigitAtLocation? {
            return others.firstOrNull { it.location.isVerticalTo(this.location) }
        }

        private fun RectF.isVerticalTo(other: RectF): Boolean {
            return this.left.between(other.left, other.right) ||
                    this.right.between(other.left, other.right)
        }

        private inline fun Float.between(v1: Float, v2: Float) = this in v1..v2

        private data class SerialGrayItem(val serialId: Int, val gray: Mat)

        fun noDetections() = ScanResult(listOf(), listOf(), null)

        private fun List<SerialGrayItem>.bySerialId(serialId: Int): List<SerialGrayItem> {
            val firstSerialId = this[0].serialId
            val serialIdIndex = serialId - firstSerialId
            if (serialIdIndex == -1) {
                "callId $callId. serialIdIndex == -1. serialId=$serialId firstSerialId=$firstSerialId".log(
                    "NonblockingCounterReadingScanner"
                )
            }
            return subList(serialIdIndex, lastIndex + 1)
            // return this.filter { it.serialId >= serialId }
        }
    }
}

private class BitmapToMats {
    private val rgbaBuffer = Mat()
    private val rgbBuffer = Mat()

    fun convert(image: Bitmap): Pair<Mat, Mat> {
        Utils.bitmapToMat(image, rgbaBuffer, true)
        Imgproc.cvtColor(rgbaBuffer, rgbBuffer, Imgproc.COLOR_RGBA2RGB)
        return rgbBuffer to rgbBuffer.rgb2gray()
    }
}

