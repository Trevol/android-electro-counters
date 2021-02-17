package com.tavrida.electro_counters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.util.Size
import androidx.camera.core.ImageProxy
import com.tavrida.counter_scanner.ImageWithId
import com.tavrida.counter_scanner.scanning.CounterScaningResult
import com.tavrida.counter_scanner.scanning.CounterScanner
import com.tavrida.counter_scanner.scanning.DetectionRoi
import com.tavrida.electro_counters.counter_scanner.CounterScannerProvider2
import com.tavrida.electro_counters.drawing.ScanResultDrawer
import org.opencv.android.OpenCVLoader


class CameraActivityController(val context: Context) {

    private object roiPaint {
        val started = Paint().apply {
            color = Color.rgb(0xFF, 0xC1, 0x07) //255,193,7 Color.rgb(0, 255, 0) //0xFFC107
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val stopped = Paint().apply {
            // color = Color.rgb(125, 63, 7) //255-130,193-130,7 Color.rgb(0, 255, 0) //0xFFC107
            color = Color.argb(55, 0xFF, 0xC1, 0x07)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
    }

    var recordingEnabled
        get() = recording.enabled
        set(value) {
            recording.enabled = value
            framesRecorder.enabled = value
        }

    private object recording {
        //global var - value should be preserved between activity recreations
        var enabled = false
    }

    val scanningStopped get() = counterScanner == null
    private inline val scanningStarted get() = !scanningStopped

    private val cameraImageConverter by lazy { CameraImageConverter2(context) }
    private val counterScannerProvider by lazy { CounterScannerProvider2() }

    private val detectorRoi = DetectionRoi(Size(400, 180))

    private var counterScanner: CounterScanner? = null
    private val storage = AppStorage(context, STORAGE_DIR)

    private val frameId = Id()
    private var framesRecorder: FramesRecorder =
        FramesRecorder(storage, enabled = recording.enabled)

    private val analyzeCycles = CallDuration()

    private val appLog = AppLog(storage)

    init {
        logDeviceInfo()
    }

    private fun logDeviceInfo() {
        if (!deviceInfoLogged) {
            appLog.deviceInfo()
            deviceInfoLogged = true
        }
    }

    fun stopScanner() {
        counterScanner?.stop()
        counterScanner = null
    }

    private fun startScanner() {
        counterScanner = counterScannerProvider.createScanner(context, detectorRoi)
        frameId.reset()
    }

    fun toggleScanning() {
        if (scanningStarted) {
            stopScanner()
        } else {
            startScanner()
        }
        framesRecorder.toggleSession(scanningStarted)
    }

    data class AnalyzeImageResult(
        val displayImage: Bitmap,
        val scanResultAndDuration: Pair<CounterScaningResult, Long>?
    )

    fun analyzeImage(image: ImageProxy): AnalyzeImageResult {
        // make local instance - because val can be updated by UI thread
        val safeScannerInstance = counterScanner
        val bitmap = image.use {
            cameraImageConverter.convert(it)
        }
        if (scanningStopped) {
            return AnalyzeImageResult(detectorRoi.draw(bitmap, roiPaint.stopped), null)
        }

        val imageWithId = ImageWithId(bitmap, frameId.next())
        framesRecorder.record(imageWithId)
        val result = safeScannerInstance!!.scan(bitmap)
        val duration = analyzeCycles.duration()
        framesRecorder.record(imageWithId.id, result, duration)

        detectorRoi.draw(bitmap, roiPaint.started)
        ScanResultDrawer().draw(bitmap, result)

        return AnalyzeImageResult(bitmap, result to duration)
    }


    private companion object {
        init {
            OpenCVLoader.initDebug()
        }

        var deviceInfoLogged = false
        private const val STORAGE_DIR = "tavrida-electro-counters"
    }
}

private class Id(val initialVal: Int = 0) {
    private var id = initialVal
    fun reset() {
        id = initialVal
    }

    fun next() = id++
}

private class CallDuration {
    private var prevCall: Long? = null

    fun duration(): Long {
        val current = System.currentTimeMillis()
        val duration = current - (prevCall ?: current)
        prevCall = current
        return duration
    }
}