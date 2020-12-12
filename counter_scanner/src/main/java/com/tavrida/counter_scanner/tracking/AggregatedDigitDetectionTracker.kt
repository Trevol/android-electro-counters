package com.tavrida.electro_counters.tracking

import com.tavrida.counter_scanner.aggregation.AggregatedDetections
import org.opencv.core.*
import com.tavrida.counter_scanner.utils.zip

class AggregatedDigitDetectionTracker {
    private val tracker = RectTracker()

    fun track(
        prevFrameGray: Mat,
        nextFrameGray: Mat,
        prevObjects: List<AggregatedDetections>
    ): List<AggregatedDetections> {
        if (prevObjects.isEmpty()){
            return listOf()
        }
        val prevBoxes = prevObjects.map { it.box }
        val (nextBoxes, statuses) = tracker.track(prevFrameGray, nextFrameGray, prevBoxes)

        val nextObjects = mutableListOf<AggregatedDetections>()
        for ((prevObject, nextBox, status) in prevObjects.zip(nextBoxes, statuses)) {
            if (!status || isAbnormalTrack(prevObject.box, nextBox)) {
                continue
            }
            nextObjects.add(AggregatedDetections(nextBox, prevObject.score, prevObject.digitsCounts))
        }

        return nextObjects
    }

    private fun isAbnormalTrack(prevBox: Rect2d, nextBox: Rect2d): Boolean {
        val wRatio = prevBox.width / nextBox.width
        val hRatio = prevBox.height / nextBox.height

        return isNormalRatio(wRatio).not() || isNormalRatio(hRatio).not()
    }

    private companion object {
        const val delta = .25
        inline fun isNormalRatio(ratio: Double) =
            1 + delta > ratio && ratio > 1 - delta
    }
}


