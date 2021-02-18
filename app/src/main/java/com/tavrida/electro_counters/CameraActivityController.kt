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
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.withLock
import kotlin.concurrent.write


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
            telemetryRecorder.enabled = value
        }

    private object recording {
        //global var - value should be preserved between activity recreations
        var enabled = false
    }

    private val cameraImageConverter by lazy { CameraImageConverter2(context) }

    private val counterScannerProvider by lazy { CounterScannerProvider2() }
    private val detectorRoi = DetectionRoi(Size(400, 180))

    private val scannerLock = ReentrantReadWriteLock()

    private var counterScanner: CounterScanner? = null

    val scanningStopped get() = scannerLock.read { counterScanner == null }
    private inline val scanningStarted get() = !scanningStopped

    private val storage = AppStorage(context, STORAGE_DIR)

    private val frameId = Id()
    private var telemetryRecorder =
        TelemetryRecorder(storage, enabled = recording.enabled)

    private val analyzeSteps = CallStep()

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

    fun stopScanner() = scannerLock.write {
        counterScanner?.stop()
        counterScanner = null
    }


    private fun startScanner() = scannerLock.write {
        counterScanner = createScanner()
        frameId.reset()
    }

    private inline fun createScanner() =
        counterScannerProvider.createScanner(context, detectorRoi, telemetryRecorder)

    fun toggleScanning() {
        if (scanningStarted) {
            stopScanner()
        } else {
            startScanner()
        }
        telemetryRecorder.toggleSession(scanningStarted)
    }

    data class AnalyzeImageResult(
        val displayImage: Bitmap,
        val scanResultAndDuration: Pair<CounterScaningResult, Long>?
    )

    fun analyzeImage(image: ImageProxy) =
        scannerLock.read {
            // make local instance - because val can be updated by UI thread
            val safeScannerInstance = counterScanner
            val bitmap = image.use {
                cameraImageConverter.convert(it)
            }
            if (scanningStopped) {
                AnalyzeImageResult(detectorRoi.draw(bitmap, roiPaint.stopped), null)
            } else {
                val imageWithId = ImageWithId(bitmap, frameId.next())
                telemetryRecorder.record(imageWithId)
                val result = safeScannerInstance!!.scan(imageWithId)
                val step = analyzeSteps.step()
                telemetryRecorder.record(imageWithId.id, result, step)

                detectorRoi.draw(bitmap, roiPaint.started)
                ScanResultDrawer().draw(bitmap, result)

                AnalyzeImageResult(bitmap, result to step)
            }
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

private class CallStep {
    private var prevCall: Long? = null

    fun step(): Long {
        val current = System.currentTimeMillis()
        val duration = current - (prevCall ?: current)
        prevCall = current
        return duration
    }
}