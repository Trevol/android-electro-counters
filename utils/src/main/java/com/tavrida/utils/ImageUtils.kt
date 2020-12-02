package com.tavrida.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.security.InvalidParameterException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

fun ImageProxy.jpeg2RgbBgrMats() =
    planes[0].buffer.toArray()
        .let { Imgcodecs.imdecode(MatOfByte(*it), Imgcodecs.IMREAD_COLOR) }
        .let { bgr -> Pair(bgr.bgr2rgb(), bgr) }


fun Bitmap.compensateSensorRotation(sensorRotationDegrees: Int) =
    if (sensorRotationDegrees == 0) {
        this.copy()
    } else
        Bitmap.createBitmap(
            this, 0, 0, this.width, this.height,
            Matrix().apply { postRotate(sensorRotationDegrees.toFloat()) },
            false
        )

fun Bitmap.copy(isMutable: Boolean = true) = copy(this.config, isMutable)

fun Bitmap.center() = Point(width / 2.0, height / 2.0)

fun Bitmap.saveAsJpeg(file: File, quality: Int = 100) = FileOutputStream(file).use { fs ->
    this.compress(Bitmap.CompressFormat.JPEG, quality, fs)
}

fun Mat.toBitmap() = Bitmap.createBitmap(cols(), rows(), Bitmap.Config.ARGB_8888)
    .apply { Utils.matToBitmap(this@toBitmap, this) }

