package com.tavrida.testsondevice

import android.content.Context
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tavrida.ElectroCounters.detection.DarknetDetector
import com.tavrida.ElectroCounters.detection.TwoStageDigitsDetector
import com.tavrida.ElectroCounters.detection.TwoStageDigitsDetectorProvider
import com.tavrida.utils.Asset
import com.tavrida.utils.assert
import com.tavrida.utils.bgr2rgb
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.utils.Converters
import org.opencv.video.SparsePyrLKOpticalFlow
import kotlin.system.measureTimeMillis


@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    companion object {
        init {
            OpenCVLoader.initDebug()
        }

        const val TAG = "DET_BENCHMARKS"
        const val frame0Name = "20201009165842_0013.jpg"
        const val frame1Name = "20201009165842_0014.jpg"

        fun appContext(): Context {
            return InstrumentationRegistry.getInstrumentation().targetContext
        }

        fun getScreenDetector(context: Context): DarknetDetector {
            TwoStageDigitsDetectorProvider(context).ensureDetector()
            return TwoStageDigitsDetectorProvider.instances.screenDetector
        }

        fun getTwoStageDetector(context: Context): TwoStageDigitsDetector {
            val provider = TwoStageDigitsDetectorProvider(context)
            provider.ensureDetector()
            return provider.detector
        }

        fun readFrames(context: Context): List<Mat> {
            val frame0 = Imgcodecs.imread(
                Asset.getFilePath(context, frame0Name, false),
                Imgcodecs.IMREAD_GRAYSCALE
            )
            val frame1 = Imgcodecs.imread(
                Asset.getFilePath(context, frame1Name, false),
                Imgcodecs.IMREAD_GRAYSCALE
            )
            return listOf(frame0, frame1)
        }

        private val optflow = SparsePyrLKOpticalFlow.create()
        fun trackPoints(
            prevImg: Mat,
            nextImg: Mat,
            prevPts: List<Point>
        ): Pair<List<Point>, List<Byte>> {
            val matOfNextPts = MatOfPoint2f()
            val matOfStatuses = MatOfByte()
            val matOfPrevPts = Converters.vector_Point2f_to_Mat(prevPts)
            optflow.calc(prevImg, nextImg, matOfPrevPts, matOfNextPts, matOfStatuses)
            val nextPts = mutableListOf<Point>().apply {
                Converters.Mat_to_vector_Point2f(
                    matOfNextPts,
                    this
                )
            }
            val statuses = mutableListOf<Byte>()
                .apply { Converters.Mat_to_vector_uchar(matOfStatuses, this) }
            return nextPts to statuses
        }

    }

    @Test
    fun detectionBenchmark() {
        val appContext = appContext()

        val testFrame = Imgcodecs.imread(Asset.getFilePath(appContext, frame0Name, false)).bgr2rgb()
        val detector = getScreenDetector(appContext)

        for (i in 0..10) {
            val timeMs = measureTimeMillis { detector.detect(testFrame) }
            Log.i(TAG, "TIME_OF_DETECTION $timeMs")
        }

        val twoStageDetector = getTwoStageDetector(appContext)
        for (i in 0..10) {
            val timeMs = measureTimeMillis { twoStageDetector.detect(testFrame) }
            Log.i(TAG, "TIME_OF_DETECTION two-stage $timeMs")
        }
    }

    @Test
    fun pointTrackingBenchmark() {
        val appContext = appContext()
        val (frame0, frame1) = readFrames(appContext)

        val pt1 = Point(569.0, 362.0)
        val pt2 = Point(583.0, 384.0)
        val n = 30
        val xStep = 2
        val yStep = 0
        val pts = (0..n).map { i -> Point(pt1.x + i * xStep, pt1.y + i * yStep) } +
                (0..n).map { i -> Point(pt2.x + i * xStep, pt2.y + i * yStep) }
        Log.i(TAG, "${pts.size}")
        for (i in 0..10) {
            val timeMs = measureTimeMillis {
                val (nextPts, statuses) = trackPoints(frame0, frame1, pts)
                // (nextPts.size == pts.size).assert()
            }
            Log.i(TAG, "TIME_OF_TRACKING $timeMs")
        }
    }

    class RectTracker {
        data class Result(val nextBoxes: List<Rect2d>, val statuses: List<Boolean>)

        fun track(prevImg: Mat, nextImg: Mat, prevBoxes: List<Rect2d>): Result {
            if (prevBoxes.isEmpty()) {
                return Result(listOf(), listOf())
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

            return Result(nextBoxes, statuses)
        }

        fun trackPoints(
            prevImg: Mat,
            nextImg: Mat,
            prevPts: List<Point>
        ): Pair<List<Point>, List<Byte>> {
            val matOfNextPts = MatOfPoint2f()
            val matOfStatuses = MatOfByte()
            val matOfPrevPts = Converters.vector_Point2f_to_Mat(prevPts)
            optflow.calc(prevImg, nextImg, matOfPrevPts, matOfNextPts, matOfStatuses)
            val nextPts = mutableListOf<Point>().apply {
                Converters.Mat_to_vector_Point2f(
                    matOfNextPts,
                    this
                )
            }
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

    @Test
    fun rectTrackingBenchmark() {
        val (frame0, frame1) = readFrames(appContext())

        val pt1 = Point(569.0, 362.0)
        val pt2 = Point(583.0, 384.0)
        val n = 100
        val xStep = 2
        val yStep = 1

        val boxes = (0..n).map { i ->
            Rect2d(
                Point(pt1.x + i * xStep, pt1.y + i * yStep),
                Point(pt2.x + i * xStep, pt2.y + i * yStep)
            )
        }
        Log.i(TAG, "${boxes.size}")
        val tracker = RectTracker()
        for (i in 0..10) {
            val timeMs = measureTimeMillis {
                val (nextBoxes, statuses) = tracker.track(frame0, frame1, boxes)
                // (nextBoxes.size == boxes.size).assert()
            }
            Log.i(TAG, "TIME_OF_TRACKING $timeMs")
        }
    }

}