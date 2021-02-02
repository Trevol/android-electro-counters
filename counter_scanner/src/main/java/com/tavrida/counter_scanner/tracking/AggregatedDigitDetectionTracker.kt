package com.tavrida.electro_counters.tracking

import android.graphics.RectF
import com.tavrida.counter_scanner.aggregation.AggregatedDetections
import com.tavrida.counter_scanner.utils.assert
import com.tavrida.counter_scanner.utils.zip
import org.opencv.core.Mat

class AggregatedDigitDetectionTracker {
    private val tracker = RectTracker()

    fun track(
        prevFrameGray: Mat,
        nextFrameGray: Mat,
        prevObjects: List<AggregatedDetections>
    ): List<AggregatedDetections> {
        if (prevObjects.isEmpty()) {
            return listOf()
        }
        val prevBoxes = prevObjects.map { it.location }
        val (nextBoxes, statuses) = tracker.track(prevFrameGray, nextFrameGray, prevBoxes)

        val nextObjects = mutableListOf<AggregatedDetections>()
        for ((prevObject, nextBox, status) in prevObjects.zip(nextBoxes, statuses)) {
            if (!status || isAbnormalTrack(prevObject.location, nextBox)) {
                continue
            }
            nextObjects.add(
                AggregatedDetections(
                    nextBox,
                    prevObject.score,
                    prevObject.digitsCounts
                )
            )
        }

        return nextObjects
    }

    fun track(
        imageSequence: List<Mat>,
        prevDetections: List<AggregatedDetections>
    ): List<AggregatedDetections> {
        (imageSequence.size >= 2).assert()
        if (prevDetections.isEmpty())
            return prevDetections

        var detections = prevDetections

        for (i in 1..imageSequence.lastIndex) {
            detections = track(imageSequence[i - 1], imageSequence[i], detections)
            if (detections.isEmpty())
                break
        }
        return detections
    }

    fun track(
        prevImg: Mat,
        nextImgs: List<Mat>,
        prevDetections: List<AggregatedDetections>
    ): List<AggregatedDetections> {
        nextImgs.isNotEmpty().assert()
        if (prevDetections.isEmpty())
            return prevDetections

        val imgSequence = mutableListOf(prevImg).apply { addAll(nextImgs) }
        return track(imgSequence, prevDetections)
    }


    private fun isAbnormalTrack(prevBox: RectF, nextBox: RectF): Boolean {
        val wRatio = prevBox.width() / nextBox.width()
        val hRatio = prevBox.height() / nextBox.height()

        return isNormalRatio(wRatio).not() || isNormalRatio(hRatio).not()
    }

    private companion object {
        const val delta = .25
        inline fun isNormalRatio(ratio: Float) =
            1 + delta > ratio && ratio > 1 - delta
    }
}


