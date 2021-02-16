package com.tavrida.electro_counters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.util.Size
import androidx.camera.core.ImageProxy
import com.tavrida.counter_scanner.scanning.CounterScaningResult
import com.tavrida.counter_scanner.scanning.CounterScanner
import com.tavrida.counter_scanner.scanning.DetectionRoi
import com.tavrida.electro_counters.counter_scanner.CounterScannerProvider2
import com.tavrida.electro_counters.drawing.ScanResultDrawer
import org.opencv.android.OpenCVLoader
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


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

    private var framesRecorder: FramesRecorder =
        FramesRecorder(storage, enabled = recording.enabled)

    fun stopScanner() {
        counterScanner?.stop()
        counterScanner = null
    }

    fun toggleScanning() {
        if (scanningStarted) {
            stopScanner()
        } else {
            counterScanner = counterScannerProvider.createScanner(context, detectorRoi)
        }
        framesRecorder.toggleSession(scanningStarted)
    }

    data class AnalyzeImageResult(
        val displayImage: Bitmap,
        val scanResultAndDuration: Pair<CounterScaningResult, Long>?
    )

    var prevAnalyzeImageCallMs: Long? = null
    private inline fun durationFromPrevCall(): Long {
        val currentAnalyzeImageCallMs = System.currentTimeMillis()
        val callDuration =
            currentAnalyzeImageCallMs - (prevAnalyzeImageCallMs ?: currentAnalyzeImageCallMs)
        prevAnalyzeImageCallMs = currentAnalyzeImageCallMs
        return callDuration
    }

    fun analyzeImage(image: ImageProxy): AnalyzeImageResult {
        val scanner = counterScanner
        val bitmap = image.use {
            cameraImageConverter.convert(it)
        }
        if (scanningStopped) {
            TODO("Move Thread.sleep(150) outside of controller")
            Thread.sleep(150) //slow down fps in stopped mode
            return AnalyzeImageResult(detectorRoi.draw(bitmap, roiPaint.stopped), null)
        }

        framesRecorder.addFrame(bitmap)
        val result = scanner!!.scan(bitmap)

        detectorRoi.draw(bitmap, roiPaint.started)
        ScanResultDrawer().draw(bitmap, result)

        return AnalyzeImageResult(bitmap, result to durationFromPrevCall())
    }


    private companion object {
        init {
            OpenCVLoader.initDebug()
        }

        private const val STORAGE_DIR = "tavrida-electro-counters"
    }
}