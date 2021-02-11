package com.tavrida.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.PointF

import java.io.File
import java.io.FileOutputStream

fun Bitmap.compensateSensorRotation(sensorRotationDegrees: Int) =
    if (sensorRotationDegrees == 0) {
        // this.copy()
        this
    } else
        Bitmap.createBitmap(
            this, 0, 0, this.width, this.height,
            Matrix().apply { postRotate(sensorRotationDegrees.toFloat()) },
            false
        )

fun Bitmap.copy(isMutable: Boolean = true) = copy(this.config, isMutable)

fun Bitmap.center() = Point(width / 2, height / 2)
fun Bitmap.exactCenter() = PointF(width / 2f, height / 2f)

fun Bitmap.saveAsJpeg(file: File, quality: Int = 100) = FileOutputStream(file).use { fs ->
    this.compress(Bitmap.CompressFormat.JPEG, quality, fs)
}
