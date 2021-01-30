package com.tavrida.counter_scanner.scanning

import com.tavrida.counter_scanner.aggregation.AggregatedDetections
import com.tavrida.counter_scanner.aggregation.AggregatingBoxGroupingDigitExtractor
import com.tavrida.counter_scanner.aggregation.DigitAtBox
import com.tavrida.counter_scanner.utils.rgb2gray
import com.tavrida.electro_counters.detection.tflite.new_detector.TfliteDetector
import com.tavrida.electro_counters.tracking.AggregatedDigitDetectionTracker
import org.opencv.core.Mat
import org.opencv.core.Rect2d
import java.io.Closeable
import java.util.*
import kotlin.IllegalStateException
import kotlin.collections.ArrayList

class NonblockingCounterReadingScanner(
    detector: TfliteDetector,
    val readingStabilityThresholdMs: Long
) : Closeable {
    data class ScanResult(
        val digitsAtBoxes: List<DigitAtBox>,
        val aggregatedDetections: List<AggregatedDetections>,
        val readingInfo: ReadingInfo?
    ) {
        data class ReadingInfo(val reading: String, val millisecondsOfStability: Long)
    }

    var stopped = false

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

    fun scan(rgbImg: Mat): ScanResult {
        ensureStarted()
        val grayImg = rgbImg.rgb2gray()

        detectorJob.input.put(DetectorJobInputItem(serialSeq, rgbImg, grayImg))
        if (prevFrameItems.isEmpty()) {
            // special processing of first frame
            // no prev frame and detections to continue processing - so skipping processing
            prevFrameItems.add(SerialGrayItem(serialSeq, grayImg))
            return noDetections()
        }
        // TODO: specify timeout here - scanner blocked for infinite time if detector job stopped
        //      unexpectedly. Or handle exceptions in detector job
        val detectorResult = detectorJob.output.keepLast()
        if (detectorResult != null) {
            val frames = prevFrameItems.bySerialId(detectorResult.serialId)
                .map { it.gray }
                .toMutableList()
            frames.add(grayImg)
            actualDetections = detectionTracker.track(frames, detectorResult.detections)
            prevFrameItems.clear()
        } else {
            val prevGray = prevFrameItems.last().gray
            actualDetections = detectionTracker.track(prevGray, grayImg, actualDetections)
        }

        prevFrameItems.add(SerialGrayItem(serialSeq, grayImg))

        serialSeq++
        val digitsAtBoxes = digitExtractor.extractDigits(actualDetections)
        val readingInfo = readingInfo(digitsAtBoxes)
        return ScanResult(digitsAtBoxes, actualDetections, readingInfo)
    }

    var prevReading = ""
    var startOfReadingMs = -1L
    private fun readingInfo(digitsAtBoxes: List<DigitAtBox>): ScanResult.ReadingInfo? {
        if (digitsAtBoxes.isEmpty())
            return null
        val reading = digitsAtBoxes.toReading()
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
        private fun List<DigitAtBox>.toReading(): String {
            return sortedBy { it.box.x }
                .removeVerticalDigits()
                .map { it.digit }
                .joinToString("") { it.toString() }
        }

        private fun List<DigitAtBox>.removeVerticalDigits(): List<DigitAtBox> {
            if (size <= 1)
                return this
            val resultItems = ArrayList<DigitAtBox>(size)
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

        private fun chooseVerticallyLower(d1: DigitAtBox, d2: DigitAtBox) =
            if (d1.box.y > d2.box.y) d1 else d2

        private fun DigitAtBox.getVertical(others: Iterable<DigitAtBox>): DigitAtBox? {
            return others.firstOrNull { it.box.isVerticalTo(this.box) }
        }

        private fun Rect2d.isVerticalTo(other: Rect2d): Boolean {
            return this.x.between(other.x, other.br().x) || this.br().x.between(
                other.x,
                other.br().x
            )
        }

        private inline fun Double.between(v1: Double, v2: Double) = this in v1..v2

        private data class SerialGrayItem(val serialId: Int, val gray: Mat)

        fun noDetections() = ScanResult(listOf(), listOf(), null)

        private fun List<SerialGrayItem>.bySerialId(serialId: Int): List<SerialGrayItem> {
            val firstSerialId = this[0].serialId
            val serialIdIndex = serialId - firstSerialId
            return subList(serialIdIndex, lastIndex + 1)
            // return this.filter { it.serialId >= serialId }
        }
    }
}

