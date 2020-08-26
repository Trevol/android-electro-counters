package com.tavrida.utils

import android.os.Build

fun deviceName(): String {
    val manufacturer: String = Build.MANUFACTURER
    val model: String = Build.MODEL
    return "$manufacturer $model"
}

fun androidInfo(): String {
    return "${Build.VERSION.SDK_INT} ${Build.VERSION.RELEASE} ${Build.VERSION.CODENAME}"
}