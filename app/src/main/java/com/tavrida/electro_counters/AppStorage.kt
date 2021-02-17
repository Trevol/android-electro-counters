package com.tavrida.electro_counters

import android.content.Context
import android.os.Environment
import java.io.File

class AppStorage(context: Context, storageDir: String) {
    val root: File = getRootDir(context, storageDir)

    private fun getRootDir(context: Context, storageDir: String): File {
        val (extDir) = tryExternalStorage(storageDir)
        return extDir ?: context.filesDir
    }


    private fun tryExternalStorage(dirName: String): ValueOrError<File> {
        try {
            if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
                return ValueOrError.error(
                    "ExternalStorage unavailable: ${Environment.getExternalStorageState()}"
                )
            }
            val extDir = File(Environment.getExternalStorageDirectory(), dirName)
            extDir.mkdir()
            checkRwAccess(extDir)
            return ValueOrError.value(extDir)
        } catch (e: Exception) {
            return ValueOrError.error(e.message)
        }
    }

    private fun checkRwAccess(extDir: File) {
        File(extDir, "test.txt").apply {
            createNewFile()
            delete()
        }
    }
}

private data class ValueOrError<T>(val value: T?, val error: String?) {
    companion object Default {
        fun <T> error(err: String?) = ValueOrError(null as T?, err)
        fun <T> value(value: T) = ValueOrError(value, null)
    }
}

