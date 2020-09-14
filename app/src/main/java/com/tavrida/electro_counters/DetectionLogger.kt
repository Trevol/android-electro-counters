package com.tavrida.electro_counters

import android.graphics.Bitmap
import android.graphics.RectF
import com.tavrida.ElectroCounters.detection.ObjectDetectionResult
import com.tavrida.ElectroCounters.detection.TwoStageDetectionResult
import com.tavrida.utils.saveAsJpeg
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class DetectionLogger(loggingEnabled: Boolean, val logDir: File) {
    private var sessionId: String = ""
    private var detectionId: Int = 0

    var loggingEnabled = loggingEnabled
        set(isEnabled) {
            if (field == isEnabled) {
                return
            }
            field = isEnabled
            toggleLogging(isEnabled)
        }
    private val loggingDisabled
        get() = !loggingEnabled

    private fun toggleLogging(enabled: Boolean) {
        detectionId = 0
        sessionId = if (enabled) createTimestamp() else ""
        var d: Boolean
        if (enabled) {
            TODO("check dir existence and creation")
            val r = logDir.mkdirs()
            d = r
        }
    }

    private fun saveImages(
        imageId: Int,
        inputImage: Bitmap,
        screenImage: Bitmap,
        digitsImage: Bitmap
    ) {
        /*val framesDir = File(filesDir, "results").apply { mkdirs() }
        val num = imageId.padStartEx(4, '0')
        inputImage.saveAsJpeg(File(framesDir, "${num}_input.jpg"))
        screenImage.saveAsJpeg(File(framesDir, "${num}_screen.jpg"))
        digitsImage.saveAsJpeg(File(framesDir, "${num}_digits.jpg"))*/
    }

    fun log(
        detectionResult: TwoStageDetectionResult,
        inputBitmap: Bitmap,
        inputBitmapWithDrawing: Bitmap,
        screenImageWithDrawing: Bitmap,
        digitsDetectionBitmap: Bitmap,
        duration: Long
    ) {
        if (loggingDisabled) {
            return
        }
        val pref = "${sessionId}_${detectionId}"
        val file = { suf: String -> File(logDir, "${pref}_$suf.jpg") }

        inputBitmap.saveAsJpeg(file("input"))
        detectionResult.screenImage.saveAsJpeg(file("screen"))
        inputBitmapWithDrawing.saveAsJpeg(file("inputDrawing"))
        screenImageWithDrawing.saveAsJpeg(file("screenDrawing"))
        digitsDetectionBitmap.saveAsJpeg(file("digits"))
        log(pref, detectionResult, duration)
        detectionId++
    }

    fun log(inputBitmap: Bitmap, duration: Long) {
        if (loggingDisabled) {
            return
        }
        val pref = "${sessionId}_${detectionId}"
        inputBitmap.saveAsJpeg(File(logDir, "${pref}_input.jpg"))
        log(pref, null, duration)
        detectionId++
    }

    private fun log(
        pref: String,
        detectionResult: TwoStageDetectionResult?,
        duration: Long
    ) {
        val sb = StringBuilder()
        sb.appendln("---duration").appendln(duration)
        if (detectionResult != null) {
            sb.appendln("---screenLocation").appendln(detectionResult.screenLocation.toLogString())
            sb.appendln("---digits")
            for (d in detectionResult.digitsDetections) {
                sb.appendln(d.toLogString())
            }
        }

        val file = File(logDir, "${pref}_detectionResult.txt")
        FileOutputStream(file).use { fs ->
            fs.writer().use { wr ->
                wr.write(sb.toString())
            }
        }
    }

    private companion object {
        fun RectF.toLogString() = "$left $top $right $bottom"
        private fun ObjectDetectionResult.toLogString(): String {
            return "${box.x} ${box.y} ${box.width} ${box.height} $classId $classScore"
        }

        private const val TIMESTAMP_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private fun createTimestamp() =
            SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US).format(System.currentTimeMillis())
    }
}
