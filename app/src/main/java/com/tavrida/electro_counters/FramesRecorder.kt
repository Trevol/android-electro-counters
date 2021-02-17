package com.tavrida.electro_counters

import android.graphics.Bitmap
import com.tavrida.utils.Timestamp
import com.tavrida.utils.saveAsJpeg
import com.tavrida.utils.zeroPad
import java.io.File

class FramesRecorder(storage: AppStorage, subDir: String = "frames", var enabled: Boolean) {
    private val framesDir = File(storage.root, subDir)
    private var sessionDir = File("", "")
    private var sessionDirCreated = false

    private var framesPos = 0
    private var sessionId = ""

    fun toggleSession(started: Boolean) {
        if (started) {
            newSession()
        }
    }

    private fun newSession() {
        framesPos = 0
        sessionId = Timestamp.current()
        sessionDirCreated = false
        sessionDir = File(framesDir, sessionId)
    }

    fun record(frame: Bitmap) {
        if (!enabled) {
            return
        }
        checkMaxFrames()
        createSessionDirIfNeeded()
        framesPos.zeroPad(FRAME_POS_LEN)
            .let { paddedPos ->
                File(sessionDir, "${paddedPos}.jpg")
            }.also { f ->
                frame.saveAsJpeg(f, JPEG_QUALITY)
            }

        framesPos++
    }

    private fun createSessionDirIfNeeded() {
        if (sessionDirCreated) {
            return
        }
        sessionDir!!.mkdirs()
    }

    private fun checkMaxFrames() {
        if (framesPos > 99999) {
            throw IllegalStateException("framesPos > 99999")
        }
    }

    private companion object {
        const val MAX_FRAMES_PER_SESSION = 99999
        const val FRAME_POS_LEN = MAX_FRAMES_PER_SESSION.toString().length

        private const val JPEG_QUALITY = 75
    }

}

