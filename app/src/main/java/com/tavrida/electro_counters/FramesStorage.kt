package com.tavrida.electro_counters

import android.graphics.Bitmap
import android.os.Environment
import com.tavrida.utils.assert
import com.tavrida.utils.padStartEx
import com.tavrida.utils.saveAsJpeg
import com.tavrida.utils.zeroPad
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FramesStorage(val storageDir: File) {
    private val framesDir = File(storageDir, "frames")

    init {
        framesDir.mkdirs()
    }

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

data class CreateExternalStorageDirResult(val externalDir: File?, val errorReason: String?)

fun createExternalStorageDir(dirName: String): CreateExternalStorageDirResult {
    if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
        return CreateExternalStorageDirResult(
            null,
            "ExternalStorage unavailable: ${Environment.getExternalStorageState()}"
        )
    }
    val extDir = File(Environment.getExternalStorageDirectory(), dirName)
    if (extDir.exists()) {
        //TODO: check access by creating and deleting file
        val file = File(extDir, "test.txt")
        file.createNewFile()
        // file.delete()
        return CreateExternalStorageDirResult(extDir, "")
    }
    val mkdirResult = extDir.mkdir()
    if (!mkdirResult) {
        return CreateExternalStorageDirResult(null, "${extDir.absolutePath} did not created")
    }
    return CreateExternalStorageDirResult(extDir, "")
}