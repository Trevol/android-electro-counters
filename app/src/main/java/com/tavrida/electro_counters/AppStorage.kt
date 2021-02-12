package com.tavrida.electro_counters

import android.content.Context
import android.os.Environment
import java.io.File

class AppStorage(context: Context, storageDir: String) {
    val root: File

    init {
        root = getRootDir(context, storageDir)
    }

    private fun getRootDir(context: Context, storageDir: String): File {
        val (extDir) = tryExternalStorage(storageDir)
        return extDir?.let { it } ?: context.filesDir
    }

    private data class ValueOrError<T>(val value: T?, val error: String?)

    private fun tryExternalStorage(dirName: String): ValueOrError<File> {
        try {
            if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
                return ValueOrError(
                    null,
                    "ExternalStorage unavailable: ${Environment.getExternalStorageState()}"
                )
            }
            val extDir = File(Environment.getExternalStorageDirectory(), dirName)
            extDir.mkdir()
            File(extDir, "test.txt").apply {
                //TODO: check access by creating and deleting file
                createNewFile()
                delete()
            }
            return ValueOrError(extDir, "")
        } catch (e: Exception) {
            return ValueOrError(null, e.message)
        }
    }
}

