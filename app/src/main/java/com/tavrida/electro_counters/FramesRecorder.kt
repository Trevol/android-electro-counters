package com.tavrida.electro_counters

import android.graphics.Bitmap
import com.tavrida.utils.Timestamp
import com.tavrida.utils.assert
import com.tavrida.utils.saveAsJpeg
import com.tavrida.utils.zeroPad
import java.io.File

class FramesRecorder(storage: AppStorage, subDir: String = "frames", var enabled: Boolean) {
    private val framesDir = File(storage.root, subDir)
    private var sessionDir: File? = null

    private var framesPos = 0
    private var sessionId = ""
    private var started = false

    fun toggleSession(started: Boolean) =
        if (started) {
            startSession()
        } else {
            stopSession()
        }


    private fun startSession() {
        (!started).assert()
        sessionId = Timestamp.current()
        framesDir.mkdirs()
        started = true
    }

    private fun stopSession() {
        started.assert()
        framesPos = 0
        sessionId = ""
        started = false
    }

    fun record(frame: Bitmap) {
        if (!enabled) {
            return
        }
        started.assert()

        framesPos.zeroPad(5)
            .let { paddedPos ->
                "${sessionId}_${paddedPos}.jpg"
            }
            .let { fn ->
                File(framesDir, fn)
            }.also { f ->
                frame.saveAsJpeg(f, JPEG_QUALITY)
            }

        framesPos++
    }

    private companion object {
        private const val JPEG_QUALITY = 75
    }

}

