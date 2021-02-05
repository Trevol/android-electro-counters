package com.tavrida.counter_scanner.scanning

import android.graphics.Bitmap
import android.graphics.Point
import com.tavrida.counter_scanner.aggregation.AggregatedDetections
import com.tavrida.counter_scanner.aggregation.AggregatingBoxGroupingDigitExtractor
import com.tavrida.counter_scanner.detection.DigitDetectionResult
import com.tavrida.counter_scanner.detection.ScreenDigitDetectionResult
import com.tavrida.counter_scanner.detection.ScreenDigitDetector
import com.tavrida.electro_counters.tracking.AggregatedDigitDetectionTracker
import org.opencv.core.Mat
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

internal class DetectorJob(
    private val detector: ScreenDigitDetector,
    private val detectionTracker: AggregatedDigitDetectionTracker,
    private val digitExtractor: AggregatingBoxGroupingDigitExtractor,
    val skipDigitsOutsideScreen: Boolean,
    val skipDigitsNearImageEdges: Boolean,
) {
    val input = LinkedBlockingQueue<DetectorJobInputItem>()
    val output = LinkedBlockingQueue<DetectorJobOutputItem>()

    private val jobThread = startJobThread()

    private fun postprocessDigitDetections(
        detectionRoiImage: Bitmap,
        detectionResult: ScreenDigitDetectionResult
    ): List<DigitDetectionResult> {
        var digitsDetections = detectionResult.digitsDetections
        if (skipDigitsOutsideScreen) {
            val screenDetection = detectionResult.screenDetection ?: return listOf()
            digitsDetections = detectionResult.digitsDetections
                .filter { screenDetection.location.contains(it.location) }
        }
        if (skipDigitsNearImageEdges){
            TODO()
        }

        return digitsDetections
    }

    private fun detectorRoutine() {
        var aggrDetectionsForFrame = listOf<AggregatedDetections>()
        var itemForDetection = input.waitAndTakeLast()
        while (isRunning()) {

            val digitsDetections = detector.detect(
                itemForDetection.detectionRoiImage,
                itemForDetection.roiOrigin
            ).let {
                postprocessDigitDetections(itemForDetection.detectionRoiImage, it)
            }

            if (isInterrupted()) { // can be interrupted during relatively long detection stage
                break
            }
            aggrDetectionsForFrame =
                digitExtractor.aggregateDetections(digitsDetections, aggrDetectionsForFrame)

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
