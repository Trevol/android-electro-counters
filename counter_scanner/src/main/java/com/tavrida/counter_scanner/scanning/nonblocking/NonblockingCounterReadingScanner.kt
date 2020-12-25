package com.tavrida.counter_scanner.scanning.nonblocking

import com.tavrida.counter_scanner.aggregation.AggregatedDetections
import com.tavrida.counter_scanner.aggregation.AggregatingBoxGroupingDigitExtractor
import com.tavrida.counter_scanner.aggregation.DigitAtBox
import com.tavrida.counter_scanner.detection.TwoStageDigitsDetector
import com.tavrida.counter_scanner.utils.rgb2gray
import com.tavrida.electro_counters.tracking.AggregatedDigitDetectionTracker
import org.opencv.core.Mat
import org.opencv.core.Rect2d
import java.io.Closeable
import kotlin.IllegalStateException

class NonblockingCounterReadingScanner(
    detector: TwoStageDigitsDetector,
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
            //todo: неотсортированный список (this) приводит к непредсказуемому результату
            val result = mutableListOf<DigitAtBox>()
            for (i in 0 until size - 1) {
                val thisBoxedDigit = get(i)
                val restOfBoxedDigits = (i + 1 until size).map { get(it) }
                val verticalBoxedDigit = thisBoxedDigit.getVertical(restOfBoxedDigits)
                if (verticalBoxedDigit == null) {
                    result.add(thisBoxedDigit)
                } else {
                    result.add(lower(thisBoxedDigit, verticalBoxedDigit))
                }
            }
            return result
        }

        private fun lower(d1: DigitAtBox, d2: DigitAtBox) =
            if (d1.box.y > d2.box.y)
                d1
            else
                d2

        private fun DigitAtBox.getVertical(others: Iterable<DigitAtBox>): DigitAtBox? {
            return others.firstOrNull { it.box.isVerticalTo(this.box) }
        }

        private fun Rect2d.isVerticalTo(other: Rect2d): Boolean {
            return this.x.between(other.x, other.br().x) || this.br().x.between(other.x, other.br().x)
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

