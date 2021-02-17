package com.tavrida.electro_counters

import com.tavrida.utils.Timestamp
import com.tavrida.utils.androidInfo
import com.tavrida.utils.deviceName
import java.io.File
import java.lang.Exception

class AppLog(storage: AppStorage) {
    private val infoFile = File(storage.root, "info.log")
    private val errorFile = File(storage.root, "error.log")

    fun deviceInfo() {
        info("[${deviceName()}] [${androidInfo()}]")
    }

    fun info(info: String) {
        val text = "${Timestamp.current()}; $info\n"
        infoFile.appendText(text)
    }

    fun error(error: Exception) {
        val errorText = "$error\n${error.stackTraceToString()}"
        error(errorText)
    }

    fun error(error: String) {
        val text = "${Timestamp.current()}; $error\n"
        errorFile.appendText(text)
    }
}