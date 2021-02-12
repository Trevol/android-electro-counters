package com.tavrida.electro_counters

import android.graphics.Bitmap
import android.os.Environment
import com.tavrida.utils.assert
import com.tavrida.utils.saveAsJpeg
import com.tavrida.utils.zeroPad
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class FramesRecorder(storage: AppStorage, subDir: String = "frames", var enabled: Boolean) {
    private val framesDir = File(storage.root, subDir)

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
        framesDir.mkdirs()
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
        private const val TIMESTAMP_FORMAT = "yyyyMMddHHmmss"
        private fun createTimestamp() =
            SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US).format(System.currentTimeMillis())
    }

}

/*fun prepareFramesRecorder(
    externalStorageDir: String,
    internalStorageDir: File,
    subDir: String = "frames",
    enabled: Boolean = false
): FramesRecorder {
    //try to external storage
    val (storageDir, _) = createExternalStorageDir(externalStorageDir)
    if (storageDir != null) {
        return FramesRecorder(storageDir, subDir, enabled)
    }
    // fallback to internal files storage
    return FramesRecorder(internalStorageDir, subDir, enabled)
}*/

