package com.tavrida.counter_scanner.scanning.nonblocking

import com.tavrida.counter_scanner.aggregation.AggregatedDetections
import com.tavrida.counter_scanner.aggregation.AggregatingBoxGroupingDigitExtractor
import com.tavrida.counter_scanner.aggregation.DigitAtBox
import com.tavrida.counter_scanner.detection.TwoStageDigitsDetector
import com.tavrida.counter_scanner.utils.rgb2gray
import com.tavrida.electro_counters.tracking.AggregatedDigitDetectionTracker
import org.opencv.core.Mat
import java.io.Closeable
import kotlin.IllegalStateException

class NonblockingCounterReadingScanner(detector: TwoStageDigitsDetector) : Closeable {
    data class ScanResult(
        val digitsAtBoxes: List<DigitAtBox>,
        val aggregatedDetections: List<AggregatedDetections>
    )

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
        val detectorResult = detectorJob.output.keepLast()
        if (detectorResult != null) {
            val frames = prevFrameItems.bySerialId(detectorResult.serialId)
                .map { it.gray }
                .toMutableList()
            assert(frames.isNotEmpty())
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
        return ScanResult(digitsAtBoxes, actualDetections)
    }

    private companion object {
        private data class SerialGrayItem(val serialId: Int, val gray: Mat)

        fun noDetections() = ScanResult(listOf(), listOf())

        private fun List<SerialGrayItem>.bySerialId(serialId: Int): List<SerialGrayItem> {
            val firstSerialId = this[0].serialId
            val serialIdIndex = serialId - firstSerialId
            return subList(serialIdIndex, lastIndex+1)

            // return this.filter { it.serialId >= serialId }
        }
    }
}

