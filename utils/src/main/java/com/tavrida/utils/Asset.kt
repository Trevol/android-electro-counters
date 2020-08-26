package com.tavrida.utils

import android.content.Context
import android.os.Environment
import java.io.*
import kotlin.jvm.Throws

object Asset {

    fun getAssetBytes(context: Context, assetName: String) =
            context.assets.open(assetName).use { it.readBytes() }


    @Throws(IOException::class)
    fun getFilePath(filesDir: File, context: Context, assetName: String): String {
        val file = File(filesDir, assetName)
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }

        val absolutePath = context.assets.open(assetName).use { inputStream ->
            FileOutputStream(file).use { os ->
                inputStream.copyTo(os)
                os.flush()
            }
            file.absolutePath
        }
        return absolutePath
    }

    @Throws(IOException::class)
    fun getFilePath(context: Context, assetName: String, deleteOtherVersions: Boolean): String {
        val filePath = getFilePath(context.filesDir, context, assetName)
        if (deleteOtherVersions) {
            deleteOtherVersions(context.filesDir, filePath)
        }
        return filePath
    }

    private fun deleteOtherVersions(dir: File, fileName: String) {
        val fileName = File(fileName).name
        val (name, version, extension) = splitFilename(fileName)
        val filesToDelete = dir.listFiles { d, fName ->
            val del = fName != fileName && fName.endsWith(extension) && fName.startsWith(name)
            del
        }
        filesToDelete?.forEach { it.delete() }
    }

    private fun splitFilename(fileName: String): Triple<String, String, String> {
        val fileName = File(fileName) // aa.6.txt OR aa.txt
        val fileWithoutExtension = File(fileName.nameWithoutExtension) // aa.6 OR aa
        val name = fileWithoutExtension.nameWithoutExtension // aa or aa
        val version = fileWithoutExtension.extension  // 6 or ''
        val extension = fileName.extension  // txt
        return Triple(name, version, extension)
    }

    fun fileInDownloads(childObj: String = "") = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), childObj)
}