package com.tavrida.electro_counters

import com.tavrida.counter_scanner.TelemetryRecorder
import com.tavrida.counter_scanner.ImageWithId
import com.tavrida.counter_scanner.detection.DigitDetectionResult
import com.tavrida.counter_scanner.detection.ScreenDigitDetectionResult
import com.tavrida.counter_scanner.scanning.CounterScaningResult
import com.tavrida.utils.Timestamp
import com.tavrida.utils.saveAsJpeg
import com.tavrida.utils.zeroPad
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class TelemetryRecorderImpl(
    storage: AppStorage,
    subDir: String = "recording",
    var enabled: Boolean
) :
    TelemetryRecorder {

    inline val disabled get() = !enabled
    private val storageSubDir = File(storage.root, subDir)
    var sessionDir: File? = null
        private set
    private var framesRecordedInSession = 0

    fun toggleSession(started: Boolean) {
        if (started) {
            newSession()
        }
    }

    private fun newSession() {
        framesRecordedInSession = 0
        val sessionId = Timestamp.current()
        sessionDir = File(storageSubDir, sessionId).also { it.mkdirs() }
    }

    private inline fun baseName(frameId: Int) = frameId.zeroPad(FRAME_POS_LEN)

    fun record(frameWithId: ImageWithId) {
        if (disabled) {
            return
        }
        checkMaxFrames()

        baseName(frameWithId.id)
            .let { baseName ->
                File(sessionDir, "${baseName}.jpg")
            }.also { fn ->
                frameWithId.image.saveAsJpeg(fn, JPEG_QUALITY)
            }
        /*baseName(frameWithId.id)
            .let { paddedPos ->
                File(sessionDir, "${paddedPos}.pixel_data")
            }.also { f ->
                frameWithId.image.saveAsRawPixelData(f)
            }*/

        framesRecordedInSession++
    }

    fun record(frameId: Int, scanResult: CounterScaningResult, analyzeStepMs: Long) {
        @Serializable
        data class ScanResultModel(
            val frameId: Int, val scanResult: CounterScaningResult, val analyzeStepMs: Long
        )

        if (disabled) {
            return
        }
        val item = ScanResultModel(frameId, scanResult, analyzeStepMs)
        baseName(frameId).let { paddedPos ->
            File(sessionDir, "${paddedPos}.scan_result")
        }.also { f ->
            item.toJson().saveTo(f)
        }
    }

    override fun record(
        frameId: Int,
        rawDetection: ScreenDigitDetectionResult,
        finalDigitsDetections: List<DigitDetectionResult>,
        detectionMs: Long,
        trackingMs: Long
    ) {
        @Serializable
        data class DetectionsTelemetryModel(
            val frameId: Int,
            val rawDetection: ScreenDigitDetectionResult,
            val finalDigitsDetections: List<DigitDetectionResult>,
            val detectionMs: Long,
            val trackingMs: Long
        )

        if (disabled) {
            return
        }

        val item = DetectionsTelemetryModel(
            frameId,
            rawDetection,
            finalDigitsDetections,
            detectionMs,
            trackingMs
        )
        baseName(frameId).let { paddedPos ->
            File(sessionDir, "${paddedPos}.detections")
        }.also { f ->
            item.toJson().saveTo(f)
        }
    }

    private fun checkMaxFrames() {
        if (framesRecordedInSession > 99999) {
            throw IllegalStateException("framesPos > 99999")
        }
    }

    private companion object {
        const val MAX_FRAMES_PER_SESSION = 99999
        const val FRAME_POS_LEN = MAX_FRAMES_PER_SESSION.toString().length
        const val JPEG_QUALITY = 75

        private inline fun <reified T> T.toJson() = Json.encodeToString(this)

        private fun String.saveTo(file: File) = file.writeText(this)
    }

}

