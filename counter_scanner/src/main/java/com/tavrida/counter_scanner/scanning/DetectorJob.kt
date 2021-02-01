package com.tavrida.counter_scanner.scanning

import android.graphics.Bitmap
import android.graphics.Point
import com.tavrida.counter_scanner.aggregation.AggregatedDetections
import com.tavrida.counter_scanner.aggregation.AggregatingBoxGroupingDigitExtractor
import com.tavrida.counter_scanner.detection.ScreenDigitDetector
import com.tavrida.electro_counters.tracking.AggregatedDigitDetectionTracker
import com.tavrida.utils.log
import org.opencv.core.Mat
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

internal class DetectorJob(
    private val detector: ScreenDigitDetector,
    private val detectionTracker: AggregatedDigitDetectionTracker,
    private val digitExtractor: AggregatingBoxGroupingDigitExtractor
) {
    val input = LinkedBlockingQueue<DetectorJobInputItem>()
    val output = LinkedBlockingQueue<DetectorJobOutputItem>()

    private val jobThread = startJobThread()

    private fun detectorRoutine() {
        var aggrDetectionsForFrame = listOf<AggregatedDetections>()
        var itemForDetection = input.waitAndTakeLast()
        while (isRunning()) {
            val detectionsForFrame =
                detector.detect(
                    itemForDetection.detectionRoiImage,
                    itemForDetection.roiOrigin
                ).digitsDetections

            "after detector.detect".log("DetectorJob")

            if (isInterrupted()) { // can be interrupted during relatively long detection stage
                break
            }
            aggrDetectionsForFrame =
                digitExtractor.aggregateDetections(detectionsForFrame, aggrDetectionsForFrame)

            //TODO: may be exec in separate loop over inputItems - because propagation to multiple frames can take some time
            //TODO: and may be exec propagation in separate thread/job
            val frames = input.waitAndTakeAll() // wait and take all items from channel

            aggrDetectionsForFrame =
                detectionTracker.track(
                    itemForDetection.gray,
                    frames.map { it.gray },
                    aggrDetectionsForFrame
                )

            if (isInterrupted()) { // can be interrupted during relatively long propagation (multi-frame) stage
                break
            }
            itemForDetection = frames.last()

            output.put(DetectorJobOutputItem(itemForDetection.serialId, aggrDetectionsForFrame))
        }
    }

    private fun startJobThread() = thread {
        try {
            detectorRoutine()
        } catch (e: InterruptedException) {
        } finally {
            input.clear()
            output.clear()
        }
    }

    fun stop() {
        jobThread.interrupt()
    }

    private companion object {
        private inline fun isRunning() = !isInterrupted()
        private inline fun isInterrupted() = Thread.currentThread().isInterrupted
    }
}

data class DetectorJobInputItem(
    val serialId: Int,
    val detectionRoiImage: Bitmap,
    val roiOrigin: Point,
    val gray: Mat
)

data class DetectorJobOutputItem(val serialId: Int, val detections: List<AggregatedDetections>)
