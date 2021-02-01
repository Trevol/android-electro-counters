package com.tavrida.utils

import android.util.Log

const val TAG = "LogUtils_TAG"

fun String.log() = Log.d(TAG, this)
fun String.log(prefix: String) = Log.d(TAG, "$prefix: ${this}")
fun Any.log() = this.toString().log()
fun Any.log(prefix: String) = this.toString().log(prefix)