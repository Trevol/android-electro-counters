package com.tavrida.counter_scanner.scanning

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import com.tavrida.counter_scanner.aggregation.AggregatedDetections
import com.tavrida.counter_scanner.aggregation.AggregatingBoxGroupingDigitExtractor
import com.tavrida.counter_scanner.aggregation.DigitAtLocation
import com.tavrida.counter_scanner.detection.ScreenDigitDetector
import com.tavrida.counter_scanner.utils.assert
import com.tavrida.electro_counters.tracking.AggregatedDigitDetectionTracker
import com.tavrida.utils.center
import com.tavrida.utils.copy
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.Closeable
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.ArrayList
import kotlin.concurrent.withLock

class CounterScanner(
    detector: ScreenDigitDetector,
    private val detectorRoi: DetectionRoi,
    stabilityThresholdMs: Long
) : Closeable {

    private var stopped = false
    private val bitmapToMats = utils.BitmapToMats()
    private val detectionTracker = AggregatedDigitDetectionTracker()
    private val digitExtractor = AggregatingBoxGroupingDigitExtractor()
    private val detectorJob = DetectorJob(
        detector, detectionTracker, digitExtractor,
        skipDigitsOutsideScreen = true,
        skipDigitsNearImageEdges = true
    )
    private val readingInfoTracker = utils.ReadingInfoTracker(stabilityThresholdMs)
    private val consumerIdTracker = utils.ConsumerIdTracker(scanNthImage = 5, forgetAfter = 500)

    private var serialSeq = 0
    private val prevFrameItems = mutableListOf<SerialGrayItem>()
    private var actualDetections = listOf<AggregatedDetections>()

    private var firstScan = true
    private val lock = ReentrantLock()

    private fun ensureStarted() {
        if (stopped) throw IllegalStateException("Scanner is stopped")
    }

    override fun close() = stop()
    fun stop() = lock.withLock {
        ensureStarted()
        detectorJob.stop()
        prevFrameItems.clear()
        firstScan = true
        stopped = true
    }

    fun scan(inputImg: Bitmap): CounterScaningResult {
        return lock.withLock { //can be stopped (with clearing state) from other (UI)thread
            scanNolock(inputImg)
        }
    }

    private fun scanNolock(inputImg: Bitmap): CounterScaningResult {
        try {
            if (stopped) {
                return CounterScaningResult.empty()
            }

            checkQueueOverflow()

            val (detectionRoiImg, _, roiOrigin) = detectorRoi.extractImage(inputImg)
            val grayMat = bitmapToMats.convertToGrayscale(inputImg)

            consumerIdTracker.enqueue(inputImg.copy(false))

            detectorJob.input.put(
                DetectorJobInputItem(
                    serialSeq,
                    detectionRoiImg,
                    roiOrigin,
                    grayMat
                )
            )

            prevFrameItems.add(SerialGrayItem(serialSeq, grayMat))

            if (firstScan) { // special processing of first frame - no prev frame and no detections yet
                firstScan = false
                return CounterScaningResult.empty()
            }

            val detectorResult = detectorJob.output.keepLastOrNull()

            if (detectorResult != null) {
                val frames = prevFrameItems.bySerialId(detectorResult.serialId)
                    .map { it.gray }
                (frames.isNotEmpty()).assert("frames.isNotEmpty()")
                actualDetections = if (frames.size == 1) {
                    detectorResult.detections
                } else {
                    detectionTracker.track(frames, detectorResult.detections)
                }
                prevFrameItems.clearBeforeLast()
            } else {
                (prevFrameItems.size >= 2).assert()
                val prevGray = prevFrameItems.getItem(-2).gray // grayMat is last item
                actualDetections = detectionTracker.track(prevGray, grayMat, actualDetections)
            }

            val digitsAtLocations = digitExtractor.extractDigits(actualDetections)
            val readingInfo = readingInfoTracker.getInfo(digitsAtLocations)
            val consumerInfo = consumerIdTracker.consumerInfo()
            return CounterScaningResult(
                digitsAtLocations,
                actualDetections,
                readingInfo,
                consumerInfo
            )
        } finally {
            serialSeq++
        }
    }

    private inline fun checkQueueOverflow() {
        if (prevFrameItems.size > MAX_QUEUE_SIZE) {
            stop()
            throw IllegalStateException("Stopping. prevFrameItems.size > MAX_QUEUE_SIZE")
        }
    }

    private companion object {
        const val MAX_QUEUE_SIZE = 100

        private data class SerialGrayItem(val serialId: Int, val gray: Mat)

        private fun List<SerialGrayItem>.bySerialId(serialId: Int): List<SerialGrayItem> {
            val firstSerialId = this[0].serialId
            val serialIdIndex = clip(0, serialId - firstSerialId)
            return subList(serialIdIndex, lastIndex + 1)
            // return this.filter { it.serialId >= serialId }
        }

        private inline fun clip(low: Int, value: Int) =
            if (value < low) {
                low
            } else {
                value
            }

        private fun <E> MutableList<E>.clearBeforeLast() {
            if (size > 1) {
                val lastItem = last()
                clear()
                add(lastItem)
            }
        }

        private fun <E> List<E>.getItem(index: Int) =
            if (index >= 0) {
                get(index)
            } else {
                //-1 should be last item (with lastIndex)
                get(lastIndex + 1 + index)
            }
    }
}

private typealias ConsumerInfo = CounterScaningResult.ConsumerInfo
private typealias ReadingInfo = CounterScaningResult.ReadingInfo

private interface utils {

    class ReadingInfoTracker(stabilityThresholdMs: Long) {
        private val stabilizingReading = StabilizingValue("", stabilityThresholdMs)

        fun getInfo(digitsAtLocations: List<DigitAtLocation>): ReadingInfo? {
            if (digitsAtLocations.isEmpty())
                return null
            val reading = digitsAtLocations.toReading()
            stabilizingReading.value = reading

            if (stabilizingReading.stabilized)
                return ReadingInfo(
                    stabilizingReading.value,
                    stabilizingReading.stableMs
                )
            return null
        }

        private companion object {

            private fun List<DigitAtLocation>.toReading(): String {
                return sortedBy { it.location.left }
                    .removeVerticalDigits()
                    .map { it.digit }
                    .joinToString("") { it.toString() }
            }

            private fun List<DigitAtLocation>.removeVerticalDigits(): List<DigitAtLocation> {
                if (size <= 1)
                    return this
                val resultItems = ArrayList<DigitAtLocation>(size)
                val srcItems = LinkedList(this)

                while (srcItems.isNotEmpty()) {
                    val thisItem = srcItems.first()
                    val restOfItems = srcItems.subList(1, srcItems.size)
                    val verticalItem = thisItem.getVertical(restOfItems)
                    if (verticalItem == null) {
                        resultItems.add(thisItem)
                    } else {
                        resultItems.add(
                            chooseVerticallyLower(
                                thisItem,
                                verticalItem
                            )
                        )
                        srcItems.remove(verticalItem)
                    }
                    srcItems.remove(thisItem)
                }

                return resultItems
            }

            private fun chooseVerticallyLower(d1: DigitAtLocation, d2: DigitAtLocation) =
                if (d1.location.top > d2.location.top) d1 else d2

            private fun DigitAtLocation.getVertical(others: Iterable<DigitAtLocation>): DigitAtLocation? {
                return others.firstOrNull { it.location.isVerticalTo(this.location) }
            }

            private fun RectF.isVerticalTo(other: RectF): Boolean {
                return this.left.between(other.left, other.right) ||
                        this.right.between(other.left, other.right)
            }

            private inline fun Float.between(v1: Float, v2: Float) = this in v1..v2
        }
    }

    class ConsumerIdTracker(scanNthImage: Int, val forgetAfter: Long) {
        private val qrScanner = QRScanner(scanNthImage)
        private var imageCenter: Point? = null

        private val rememCustomerId = RememberedValue<String>(forgetAfter)

        fun enqueue(image: Bitmap) {
            imageCenter = image.center()
            qrScanner.postProcess(image)
        }

        fun consumerInfo(): ConsumerInfo? {
            // readingInfo needed??
            val observedInfo = observedInfo()
            if (observedInfo != null) {
                rememCustomerId.remember(observedInfo.consumerId)
                return observedInfo
            }
            if (rememCustomerId.empty() || rememCustomerId.forgetIfNeeded()) {
                return null
            }
            // TODO: time to forget - but has some detections???
            return ConsumerInfo(rememCustomerId.value(), null)
        }

        private fun observedInfo(): ConsumerInfo? {
            val barcodes = qrScanner.barcodes()
            if (barcodes.isEmpty())
                return null
            if (barcodes.size == 1)
                return ConsumerInfo(
                    barcodes[0].rawValue!!,
                    barcodes[0].boundingBox!!
                )
            val barcode = barcodes.minByOrNull { it.boundingBox.squaredDist(imageCenter!!) }!!
            return ConsumerInfo(barcode.rawValue!!, barcode.boundingBox!!)
        }

        companion object {
            inline fun Rect.squaredDist(p: Point) = center().squaredDist(p)
            inline fun Point.squaredDist(o: Point) = square(x - o.x) + square(y - o.y)
            inline fun square(a: Int) = a * a
        }
    }

    class StabilizingValue<T>(initial: T?, val stabilizationMs: Long) {
        private var _value: T? = initial
        private var startOfValue = 0L
        var value
            get() = _value!!
            set(value) {
                if (value != _value) {
                    _value = value
                    startOfValue = System.currentTimeMillis()
                }
            }
        val stableMs: Long
            get() = if (startOfValue == 0L)
                0L
            else
                System.currentTimeMillis() - startOfValue

        val stabilized get() = stableMs > stabilizationMs
    }

    class RememberedValue<T>(val forgetAfter: Long) {
        private var value: T? = null
        private var at = 0L

        fun value() = value!!

        fun empty() = value == null

        fun remember(value: T) {
            this.value = value
            at = System.currentTimeMillis()
        }

        fun forget() {
            value = null
            at = 0
        }

        fun rememberFor(): Long {
            if (at == 0L)
                return 0
            return System.currentTimeMillis() - at
        }

        fun forgetIfNeeded(): Boolean {
            if (rememberFor() > forgetAfter) {
                forget()
                return true
            }
            return false
        }
    }

    class BitmapToMats {
        private val rgbaBuffer = Mat()

        fun convertToGrayscale(image: Bitmap): Mat {
            Utils.bitmapToMat(image, rgbaBuffer, true)
            return Mat().apply { Imgproc.cvtColor(rgbaBuffer, this, Imgproc.COLOR_RGBA2GRAY) }
        }
    }
}




