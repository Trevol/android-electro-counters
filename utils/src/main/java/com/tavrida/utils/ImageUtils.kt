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

fun Mat.copy() = Mat().also { this.copyTo(it) }
fun Mat.bgr2rgbInplace() = this.also { Imgproc.cvtColor(this, it, Imgproc.COLOR_BGR2RGB) }
fun Mat.bgr2rgb() = Mat().also { rgb -> Imgproc.cvtColor(this, rgb, Imgproc.COLOR_RGB2BGR) }
fun Mat.rgb2bgr() = Mat().also { bgr -> Imgproc.cvtColor(this, bgr, Imgproc.COLOR_RGB2BGR) }
fun Mat.rgba2rgb() = Mat().also { rgba -> Imgproc.cvtColor(this, rgba, Imgproc.COLOR_RGBA2RGB) }
fun Mat.rgba2bgra() = Mat().also { rgba -> Imgproc.cvtColor(this, rgba, Imgproc.COLOR_RGBA2BGRA) }

fun Mat.roi(roi: Rect, padding: Int = 0) = this.roi(roi, padding, padding)

fun Mat.roi(roi: Rect, hPadding: Int = 0, vPadding: Int = 0): Pair<Mat, Rect> {
    val height = this.rows()
    val width = this.cols()
    val paddedX = max(roi.x - hPadding, 0)
    val paddedY = max(roi.y - vPadding, 0)
    val roi = Rect(
        paddedX,
        paddedY,
        min(roi.width + hPadding + hPadding, width - paddedX),
        min(roi.height + vPadding + vPadding, height - paddedY)
    )
    return Mat(this, roi) to roi
}

fun Mat.roi(roi: Rect, hPadding: Double = 0.0, vPadding: Double = 0.0): Pair<Mat, Rect> {
    // (hPadding >= 0.0 && vPadding >= 0.0).assert()
    val hPaddingAbs = round(roi.width * hPadding).toInt()
    val vPaddingAbs = round(roi.height * vPadding).toInt()
    return this.roi(roi, hPaddingAbs, vPaddingAbs)
}

private fun ByteBuffer.toArray() = ByteArray(this.capacity())
    .also {
        this.rewind()
        this.get(it)
    }

fun Range(s: Double, e: Double) = Range(s.toInt(), e.toInt())
fun Size(width: Int, height: Int) = Size(width.toDouble(), height.toDouble())
fun Scalar(v0: Int, v1: Int, v2: Int) = Scalar(v0.toDouble(), v1.toDouble(), v2.toDouble())
fun Scalar(v0: Int) = Scalar(v0.toDouble())


fun latterbox(
    img: Mat,
    newShape: Size = Size(416, 416),
    color: Scalar = Scalar.all(114.0),
    auto: Boolean = true,
    scaleFill: Boolean = false,
    scaleup: Boolean = true
): Triple<Mat, Size, Size> {
    var img = img
    val shape = img.size()
    var r = min(newShape.height / shape.height, newShape.width / shape.width)
    if (!scaleup)
        r = min(r, 1.0)

    // Compute padding
    var whRatio = Size(r, r)
    var newUnpad = Size(round(shape.width * r), round(shape.height * r))
    var dw = newShape.width - newUnpad.width
    var dh = newShape.height - newUnpad.height
    if (auto) {
        dw %= 64.0
        dh %= 64.0
    } else if (scaleFill) {
        dw = 0.0
        dh = 0.0
        newUnpad = newShape
        whRatio = Size(newShape.width / shape.width, newShape.height / shape.height)
    }
    dw /= 2
    dh /= 2
    if (shape != newUnpad) {
        img = Mat().also { Imgproc.resize(img, it, newUnpad, -1.0, -1.0, Imgproc.INTER_LINEAR) }
    }

    val top = round(dh - .1).toInt()
    val bottom = round(dh + .1).toInt()
    val left = round(dw - 0.1).toInt()
    val right = round(dw + 0.1).toInt()
    img = Mat().also {
        Core.copyMakeBorder(
            img,
            it,
            top,
            bottom,
            left,
            right,
            Core.BORDER_CONSTANT,
            color
        )
    }

    return Triple(img, whRatio, Size(dw, dh))
}

fun hstack(vararg mats: Mat, fillColor: Scalar = Scalar.all(0.0)): Mat {
    mats.isNotEmpty().assert()
    // stack horizontally
    val height = mats.maxBy { it.height() }!!.height()
    val width = mats.sumBy { it.width() }
    val stacked = Mat(height, width, mats[0].type(), fillColor)
    var x = 0
    for (m in mats) {
        val roi = Mat(
            stacked,
            Range(0, m.height()),
            Range(x, x + m.width())
        )
        m.copyTo(roi)
        x += m.width()
    }
    return stacked
}

fun vstack(vararg mats: Mat, fillColor: Scalar = Scalar.all(0.0)): Mat {
    mats.isNotEmpty().assert()
    // stack horizontally
    val height = mats.sumBy { it.height() }
    val width = mats.maxBy { it.width() }!!.width()
    val stacked = Mat(height, width, mats[0].type(), fillColor)
    var y = 0
    for (m in mats) {
        val roi = Mat(
            stacked,
            Range(y, y + m.height()),
            Range(0, m.width())
        )
        m.copyTo(roi)
        y += m.height()
    }
    return stacked
}

fun Mat.resize(
    width: Int? = null,
    height: Int? = null,
    interpolation: Int = Imgproc.INTER_LINEAR
): Mat {
    if (width == null && height == null)
        throw AssertionError("width == null && height == null")
    if (width != null && height != null)
        return Mat().also {
            Imgproc.resize(this, it, Size(width, height), .0, .0, interpolation)
        }
    val k = if (width != null) {
        width / this.width().toDouble()
    } else {
        height!! / this.height().toDouble()
    }
    return Mat().also {
        Imgproc.resize(this, it, Size(), k, k, interpolation)
    }
}

fun Mat.compensateSensorRotation(dst: Mat, sensorRotationDegrees: Int): Mat {
    val rotateCode = when (sensorRotationDegrees) {
        0 -> return this
        90 -> Core.ROTATE_90_CLOCKWISE
        180 -> Core.ROTATE_180
        else -> throw InvalidParameterException("Unexpected value $sensorRotationDegrees for sensorRotationDegrees")
    }
    Core.rotate(this, dst, rotateCode)
    return dst
}

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

