package com.tavrida.utils

import java.text.SimpleDateFormat
import java.util.*

object Timestamp {
    const val TIMESTAMP_FORMAT = "yyyyMMdd-HHmm-ss-SSS"
    fun current() = SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US).format(System.currentTimeMillis())
}