package com.tavrida.electro_counters

import com.tavrida.counter_scanner.DetectionsRecorder
import com.tavrida.counter_scanner.ImageWithId
import com.tavrida.counter_scanner.detection.DigitDetectionResult
import com.tavrida.counter_scanner.detection.ScreenDigitDetectionResult
import com.tavrida.counter_scanner.scanning.CounterScaningResult
import com.tavrida.utils.Timestamp
import com.tavrida.utils.saveAsJpeg
import com.tavrida.utils.saveAsRawPixelData
import com.tavrida.utils.zeroPad
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class TelemetryRecorder(storage: AppStorage, subDir: String = "recording", var enabled: Boolean) :
    DetectionsRecorder {

    inline val disabled get() = !enabled
    private val framesDir = File(storage.root, subDir)
    private var sessionDir = File("", "")
    private var sessionDirCreated = false

    private var framesRecordedInSession = 0
    private var sessionId = ""

    fun toggleSession(started: Boolean) {
        if (started) {
            newSession()
        }
    }

    private fun newSession() {
        framesRecordedInSession = 0
        sessionId = Timestamp.current()
        sessionDirCreated = false
        sessionDir = File(framesDir, sessionId)
    }

    private inline fun baseName(frameId: Int) = frameId.zeroPad(FRAME_POS_LEN)

    fun record(frameWithId: ImageWithId) {
        if (disabled) {
            return
        }
        checkMaxFrames()
        createSessionDirIfNeeded()

        /*baseName(frameWithId.id)
            .let { paddedPos ->
                File(sessionDir, "${paddedPos}.jpg")
            }.also { f ->
                frameWithId.image.saveAsJpeg(f, JPEG_QUALITY)
            }*/
        baseName(frameWithId.id)
            .let { paddedPos ->
                File(sessionDir, "${paddedPos}.pixel_data")
            }.also { f ->
                frameWithId.image.saveAsRawPixelData(f)
            }

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

    @Serializable
    internal data class RecordItem(
        val frameId: Int,
        val scanResult: CounterScaningResult,
        val analyzeStepMs: Long
    )

    private fun createSessionDirIfNeeded() {
        if (sessionDirCreated) {
            return
        }
        sessionDir!!.mkdirs()
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

