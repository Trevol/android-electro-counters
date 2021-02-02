package com.tavrida.electro_counters.tracking

import android.graphics.RectF
import androidx.core.graphics.toRect
import com.tavrida.utils.toRect2d
import com.tavrida.utils.toRectF
import org.opencv.core.*
import org.opencv.utils.Converters
import org.opencv.video.SparsePyrLKOpticalFlow

class RectTracker {
    data class ResultRect2d(val nextBoxes: List<Rect2d>, val statuses: List<Boolean>)
    data class ResultRectF(val nextBoxes: List<RectF>, val statuses: List<Boolean>)

    //TODO: avoid Rect2d. Convert to MatOfPoint from RectF directly
    fun track(prevImg: Mat, nextImg: Mat, prevBoxes: List<RectF>): ResultRectF {
        val prevBoxes = prevBoxes.map { it.toRect2d() }
        val (nextBoxes2d, statuses) = track(prevImg, nextImg, prevBoxes)
        val nextBoxes = nextBoxes2d.map { it.toRectF() }
        return ResultRectF(nextBoxes, statuses)
    }

    fun track(prevImg: Mat, nextImg: Mat, prevBoxes: List<Rect2d>): ResultRect2d {
        if (prevBoxes.isEmpty()) {
            return ResultRect2d(listOf(), listOf())
        }
        val prevPts = prevBoxes.toTrackedPts()
        val (nextPts, nextPtsStatuses) = trackPoints(prevImg, nextImg, prevPts)

        val nextPtsIter = nextPts.iterator()
        val statusesIter = nextPtsStatuses.iterator()

        val nextBoxes = mutableListOf<Rect2d>()
        val statuses = mutableListOf<Boolean>()
        while (nextPtsIter.hasNext()) {
            val tl = nextPtsIter.next()
            val br = nextPtsIter.next()
            val tlStatus = statusesIter.next()
            val brStatus = statusesIter.next()

            nextBoxes.add(Rect2d(tl, br))
            val pointsInRightOrder = tl.x < br.x && tl.y < br.y
            statuses.add(pointsInRightOrder && tlStatus == statusOk && brStatus == statusOk)
        }

        return ResultRect2d(nextBoxes, statuses)
    }

    private fun trackPoints(
        prevImg: Mat,
        nextImg: Mat,
        prevPts: List<Point>
    ): Pair<List<Point>, List<Byte>> {
        val matOfNextPts = MatOfPoint2f()
        val matOfStatuses = MatOfByte()
        val matOfPrevPts = Converters.vector_Point2f_to_Mat(prevPts)
        optflow.calc(prevImg, nextImg, matOfPrevPts, matOfNextPts, matOfStatuses)
        val nextPts =
            mutableListOf<Point>().apply { Converters.Mat_to_vector_Point2f(matOfNextPts, this) }
        val statuses =
            mutableListOf<Byte>().apply { Converters.Mat_to_vector_uchar(matOfStatuses, this) }
        return nextPts to statuses
    }

    private companion object {
        private val optflow = SparsePyrLKOpticalFlow.create()

        val statusOk: Byte = 1

        fun List<Rect2d>.toTrackedPts(): List<Point> {
            val pts = mutableListOf<Point>()
            for (box in this) {
                pts.add(box.tl())
                pts.add(box.br())
            }
            return pts
        }
    }
}
