package com.tavrida.counter_scanner.scanning

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import com.google.mlkit.vision.barcode.Barcode
import com.tavrida.counter_scanner.aggregation.AggregatedDetections
import com.tavrida.counter_scanner.aggregation.AggregatingBoxGroupingDigitExtractor
import com.tavrida.counter_scanner.aggregation.DigitAtLocation
import com.tavrida.counter_scanner.detection.ScreenDigitDetector
import com.tavrida.counter_scanner.utils.assert
import com.tavrida.electro_counters.tracking.AggregatedDigitDetectionTracker
import com.tavrida.utils.copy
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.Closeable
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.IllegalStateException
import kotlin.collections.ArrayList
import kotlin.concurrent.withLock

class NonblockingCounterReadingScanner(
    detector: ScreenDigitDetector,
    private val detectorRoi: DetectionRoi,
    stabilityThresholdMs: Long
) : Closeable {

    data class ScanResult(
        val digitsAtLocations: List<DigitAtLocation>,
        val aggregatedDetections: List<AggregatedDetections>,
        val readingInfo: ReadingInfo?,
        val consumerInfo: ConsumerInfo?
    ) {
        data class ReadingInfo(val reading: String, val msOfStability: Long)
        data class ConsumerInfo(
            val consumerId: String,
            val barcodeLocation: Rect,
            val msOfStability: Long
        )

        companion object {
            fun Empty() = ScanResult(listOf(), listOf(), null, null)
        }
    }

    private var stopped = false
    private val bitmapToMats = BitmapToMats()
    private val detectionTracker = AggregatedDigitDetectionTracker()
    private val digitExtractor = AggregatingBoxGroupingDigitExtractor()
    private val detectorJob = DetectorJob(
        detector, detectionTracker, digitExtractor,
        skipDigitsOutsideScreen = true,
        skipDigitsNearImageEdges = true
    )
    private val readingInfoTracker = ReadingInfoTracker(stabilityThresholdMs)
    private val consumerIdTracker = ConsumerIdTracker(scanNthImage = 5)

    private var serialSeq = 0
    private val prevFrameItems = mutableListOf<SerialGrayItem>()
    private var actualDetections = listOf<AggregatedDetections>()

    private var firstScan = true
    private val lock = ReentrantLock()

    private fun ensureStarted() {
        if (stopped) throw IllegalStateException("Scanner is stopped")
    }

    override fun close() = stop()
    fun stop() = lock.withLock {
        ensureStarted()
        detectorJob.stop()
        prevFrameItems.clear()
        firstScan = true
        stopped = true
    }

    fun scan(inputImg: Bitmap): ScanResult {
        return lock.withLock { //can be stopped (with clearing state) from other (UI)thread
            scanNolock(inputImg)
        }
    }

    private fun scanNolock(inputImg: Bitmap): ScanResult {
        try {
            if (stopped) {
                return ScanResult.Empty()
            }

            if (prevFrameItems.size > MAX_QUEUE_SIZE) {
                stop()
                throw IllegalStateException("Stopping. prevFrameItems.size > MAX_QUEUE_SIZE")
            }

            val (detectionRoiImg, _, roiOrigin) = detectorRoi.extractImage(inputImg)
            val grayMat = bitmapToMats.convertToGrayscale(inputImg)

            consumerIdTracker.enqueueToProcessing(inputImg.copy(false))

            detectorJob.input.put(
                DetectorJobInputItem(
                    serialSeq,
                    detectionRoiImg,
                    roiOrigin,
                    grayMat
                )
            )

            prevFrameItems.add(SerialGrayItem(serialSeq, grayMat))

            if (firstScan) { // special processing of first frame - no prev frame and no detections yet
                firstScan = false
                return ScanResult.Empty()
            }

            val detectorResult = detectorJob.output.keepLastOrNull()

            if (detectorResult != null) {
                val frames = prevFrameItems.bySerialId(detectorResult.serialId)
                    .map { it.gray }
                (frames.isNotEmpty()).assert("frames.isNotEmpty()")
                actualDetections = if (frames.size == 1) {
                    detectorResult.detections
                } else {
                    detectionTracker.track(frames, detectorResult.detections)
                }
                prevFrameItems.clearBeforeLast()
            } else {
                (prevFrameItems.size >= 2).assert()
                val prevGray = prevFrameItems.getItem(-2).gray // grayMat is last item
                actualDetections = detectionTracker.track(prevGray, grayMat, actualDetections)
            }

            val digitsAtBoxes = digitExtractor.extractDigits(actualDetections)
            val readingInfo = readingInfoTracker.getInfo(digitsAtBoxes)
            val consumerInfo = consumerIdTracker.consumerInfo()
            return ScanResult(
                digitsAtBoxes,
                actualDetections,
                readingInfo,
                consumerInfo
            )
        } finally {
            serialSeq++
        }
    }


    private companion object {
        const val MAX_QUEUE_SIZE = 100

        private data class SerialGrayItem(val serialId: Int, val gray: Mat)

        private fun List<SerialGrayItem>.bySerialId(serialId: Int): List<SerialGrayItem> {
            val firstSerialId = this[0].serialId
            val serialIdIndex = clip(0, serialId - firstSerialId)
            return subList(serialIdIndex, lastIndex + 1)
            // return this.filter { it.serialId >= serialId }
        }

        private inline fun clip(low: Int, value: Int) =
            if (value < low) {
                low
            } else {
                value
            }

        private fun <E> MutableList<E>.clearBeforeLast() {
            if (size > 1) {
                val lastItem = last()
                clear()
                add(lastItem)
            }
        }

        private fun <E> List<E>.getItem(index: Int) =
            if (index >= 0) {
                get(index)
            } else {
                //-1 should be last item (with lastIndex)
                get(lastIndex + 1 + index)
            }
    }
}

private class ReadingInfoTracker(val stabilityThresholdMs: Long) {
    private var prevReading = ""
    private var startOfReadingMs = -1L

    fun getInfo(digitsAtLocations: List<DigitAtLocation>): NonblockingCounterReadingScanner.ScanResult.ReadingInfo? {
        if (digitsAtLocations.isEmpty())
            return null
        val reading = digitsAtLocations.toReading()
        if (reading != prevReading) { // new reading available
            prevReading = reading
            startOfReadingMs = System.currentTimeMillis()
        }

        val msOfReadingStability = System.currentTimeMillis() - startOfReadingMs
        if (msOfReadingStability >= stabilityThresholdMs)
            return NonblockingCounterReadingScanner.ScanResult.ReadingInfo(
                reading,
                msOfReadingStability
            )
        return null
    }

    private companion object {

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
                    resultItems.add(
                        chooseVerticallyLower(
                            thisItem,
                            verticalItem
                        )
                    )
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
    }
}

private class ConsumerIdTracker(scanNthImage: Int) {
    fun enqueueToProcessing(image: Bitmap) {
        qrScanner.postProcess(image)
    }

    fun consumerInfo(): NonblockingCounterReadingScanner.ScanResult.ConsumerInfo? {
        // readingInfo needed??
        TODO("Not yet implemented")
    }

    private val qrScanner = QRScanner(scanNthImage)
}

private class BitmapToMats {
    private val rgbaBuffer = Mat()

    fun convertToGrayscale(image: Bitmap): Mat {
        Utils.bitmapToMat(image, rgbaBuffer, true)
        return Mat().apply { Imgproc.cvtColor(rgbaBuffer, this, Imgproc.COLOR_RGBA2GRAY) }
    }
}

