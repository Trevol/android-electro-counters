package com.tavrida.electro_counters.tracking

import android.graphics.RectF
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.utils.Converters
import org.opencv.video.SparsePyrLKOpticalFlow

class RectTracker {
    data class TrackResult(val nextBoxes: List<RectF>, val statuses: List<Boolean>)

    fun track(prevImg: Mat, nextImg: Mat, prevBoxes: List<RectF>): TrackResult {
        if (prevBoxes.isEmpty()) {
            return TrackResult(listOf(), listOf())
        }
        val prevPts = prevBoxes.toTrackedPts()
        val (nextPts, nextPtsStatuses) = trackPoints(prevImg, nextImg, prevPts)

        val nextPtsIter = nextPts.iterator()
        val statusesIter = nextPtsStatuses.iterator()

        val nextBoxes = mutableListOf<RectF>()
        val statuses = mutableListOf<Boolean>()
        while (nextPtsIter.hasNext()) {
            val tl = nextPtsIter.next()
            val br = nextPtsIter.next()
            val tlStatus = statusesIter.next()
            val brStatus = statusesIter.next()

            nextBoxes.add(RectF(tl, br))
            val pointsInRightOrder = tl.x < br.x && tl.y < br.y
            statuses.add(pointsInRightOrder && tlStatus == statusOk && brStatus == statusOk)
        }

        return TrackResult(nextBoxes, statuses)
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

        fun List<RectF>.toTrackedPts(): List<Point> {
            val pts = mutableListOf<Point>()
            for (box in this) {
                pts.add(box.tl())
                pts.add(box.br())
            }
            return pts
        }

        private inline fun RectF.tl() = Point(left, top)
        private inline fun RectF.br() = Point(right, bottom)
        private inline fun Point(x: Float, y: Float) = Point(x.toDouble(), y.toDouble())
        private inline fun RectF(tl: Point, br: Point) = RectF(tl.x, tl.y, br.x, br.y)
        private inline fun RectF(left: Double, top: Double, right: Double, bottom: Double) =
            RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
    }
}
