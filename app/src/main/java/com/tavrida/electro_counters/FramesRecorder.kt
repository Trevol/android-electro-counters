package com.tavrida.electro_counters

import com.tavrida.counter_scanner.ImageWithId
import com.tavrida.counter_scanner.scanning.CounterScaningResult
import com.tavrida.utils.Timestamp
import com.tavrida.utils.saveAsJpeg
import com.tavrida.utils.zeroPad
import java.io.File

class FramesRecorder(storage: AppStorage, subDir: String = "frames", var enabled: Boolean) {
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

    fun record(frameWithId: ImageWithId) {
        if (disabled) {
            return
        }
        checkMaxFrames()
        createSessionDirIfNeeded()
        frameWithId.id.zeroPad(FRAME_POS_LEN)
            .let { paddedPos ->
                File(sessionDir, "${paddedPos}.jpg")
            }.also { f ->
                frameWithId.image.saveAsJpeg(f, JPEG_QUALITY)
            }

        framesRecordedInSession++
    }

    fun record(frameId: Int, result: CounterScaningResult, analizeImageDuration: Long) {
        if (disabled) {
            return
        }
        // result.seri
        TODO("record raw detections!!!!")
        TODO("record timings!!!")
        TODO()
    }

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

        private const val JPEG_QUALITY = 75
    }

}

