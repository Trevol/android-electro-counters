package com.tavrida.counter_scanner.scanning

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.RectF
import android.util.Size
import com.tavrida.counter_scanner.TelemetryRecorder
import com.tavrida.counter_scanner.aggregation.AggregatedDetections
import com.tavrida.counter_scanner.aggregation.AggregatingBoxGroupingDigitExtractor
import com.tavrida.counter_scanner.detection.DigitDetectionResult
import com.tavrida.counter_scanner.detection.ScreenDigitDetectionResult
import com.tavrida.counter_scanner.detection.ScreenDigitDetector
import com.tavrida.electro_counters.tracking.AggregatedDigitDetectionTracker
import com.tavrida.utils.RectF
import com.tavrida.utils.size
import org.opencv.core.Mat
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

internal class DetectorJob(
    private val detector: ScreenDigitDetector,
    private val detectionTracker: AggregatedDigitDetectionTracker,
    private val digitExtractor: AggregatingBoxGroupingDigitExtractor,
    val skipDigitsOutsideScreen: Boolean,
    val skipDigitsNearImageEdges: Boolean,
    val recorder: TelemetryRecorder?,
) {
    val input = LinkedBlockingQueue<DetectorJobInputItem>()
    val output = LinkedBlockingQueue<DetectorJobOutputItem>()

    private val jobThread = startJobThread()

    private fun postprocessDigitDetections(
        roiSize: Size,
        roiOrigin: Point,
        detectionResult: ScreenDigitDetectionResult
    ): List<DigitDetectionResult> {
        var digitsDetections = detectionResult.digitsDetections
        val screenDetection = detectionResult.screenDetection ?: return listOf()
        if (skipDigitsOutsideScreen) {
            digitsDetections = digitsDetections
                .filter { screenDetection.location.contains(it.location) }
        }
        if (skipDigitsNearImageEdges) {
            // TODO("skip screen (and digits in it) if screen near detection region edges")
            val screenInsideRoi = screenDetection.location.insideRoi(roiSize,
                roiOrigin,
                ROI_PADDING)
            if (!screenInsideRoi){
                return listOf()
            }
            digitsDetections = digitsDetections
                .filter {
                    it.location.insideRoi(
                        roiSize,
                        roiOrigin,
                        ROI_PADDING
                    )
                }

        }


        return digitsDetections
    }

    private fun detectorRoutine() {
        var aggrDetectionsForFrame = listOf<AggregatedDetections>()
        var itemForDetection = input.waitAndTakeLast()

        while (isRunning()) {

            val (detectionMs, detectionResult) = measureTimedValue {
                detector.detect(
                    itemForDetection.detectionRoiImage,
                    itemForDetection.roiOrigin
                )
            }

            val finalDigitsDetections =
                postprocessDigitDetections(
                    itemForDetection.detectionRoiImage.size,
                    itemForDetection.roiOrigin,
                    detectionResult
                )

            if (isInterrupted()) { // can be interrupted during relatively long detection stage
                break
            }
            aggrDetectionsForFrame =
                digitExtractor.aggregateDetections(finalDigitsDetections, aggrDetectionsForFrame)

            //TODO: may be exec in separate loop over inputItems - because propagation to multiple frames can take some time
            //TODO: and may be exec propagation in separate thread/job
            val frames = input.waitAndTakeAll() // wait and take all items from channel

            val trackingMs = measureTimeMillis {
                aggrDetectionsForFrame =
                    detectionTracker.track(
                        itemForDetection.gray,
                        frames.map { it.gray },
                        aggrDetectionsForFrame
                    )
            }

            if (isInterrupted()) { // can be interrupted during relatively long propagation (multi-frame) stage
                break
            }
            itemForDetection = frames.last()

            recorder?.record(
                itemForDetection.frameId,
                detectionResult,
                finalDigitsDetections,
                detectionMs, trackingMs
            )

            output.put(
                DetectorJobOutputItem(
                    itemForDetection.serialId,
                    itemForDetection.frameId,
                    aggrDetectionsForFrame
                )
            )
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

        const val ROI_PADDING = 10

        private fun RectF.insideRoi(
            roiSize: Size,
            roiOrigin: Point,
            padding: Int
        ): Boolean {
            val imgRect = RectF(
                roiOrigin.x + padding, roiOrigin.y + padding,
                roiOrigin.x + roiSize.width - padding,
                roiOrigin.y + roiSize.height - padding
            )
            return imgRect.contains(this)
        }

        data class TimedValue<T>(val timeMs: Long, val value: T)

        fun <T> measureTimedValue(block: () -> T): TimedValue<T> {
            val t0 = System.currentTimeMillis()
            val value = block()
            val t1 = System.currentTimeMillis()
            return TimedValue(t1 - t0, value)
        }
    }
}

data class DetectorJobInputItem(
    val serialId: Int,
    val frameId: Int,
    val detectionRoiImage: Bitmap,
    val roiOrigin: Point,
    val gray: Mat
)

data class DetectorJobOutputItem(
    val serialId: Int,
    val frameId: Int,
    val detections: List<AggregatedDetections>
)
