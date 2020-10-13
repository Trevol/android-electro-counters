package com.tavrida.electro_counters

import android.graphics.Bitmap
import com.tavrida.utils.assert
import com.tavrida.utils.padStartEx
import com.tavrida.utils.saveAsJpeg
import com.tavrida.utils.zeroPad
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FramesStorage(val storageDir: File) {
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
        storageDir.mkdirs()
        sessionId = createTimestamp()
        started = true
    }

    private fun stopSession() {
        started.assert()
        framesPos = 0
        sessionId = ""
        started = false
    }

    fun addFrame(frame: Bitmap) {
        started.assert()
        val paddedPos = framesPos.zeroPad(4)
        val fileName = "${sessionId}_${paddedPos}.jpg"
        frame.saveAsJpeg(File(storageDir, fileName))
        framesPos++
    }

    private companion object {
        private const val TIMESTAMP_FORMAT = "yyyyMMddHHmmss"
        private fun createTimestamp() =
            SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US).format(System.currentTimeMillis())
    }

}