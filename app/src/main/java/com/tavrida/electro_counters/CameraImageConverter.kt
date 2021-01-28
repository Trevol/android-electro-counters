package com.tavrida.electro_counters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.camera.core.ImageProxy
import com.tavrida.utils.camera.YuvToRgbConverter2
import com.tavrida.utils.compensateSensorRotation
import java.lang.IllegalStateException

class CameraImageConverter(context: Context) {
    private val conversionBuffer = BitmapProxy()
    private var displayBuffer = BitmapProxy()
    private val yuvToRgbConverter = YuvToRgbConverter2(context)

    data class ConvertResult(val readyForProcessing: Bitmap, val readyForDisplay: Bitmap)

    private fun ImageProxy.fromYuvToRgb(): Bitmap {
        val converted = conversionBuffer.bitmapLike(this)
        yuvToRgbConverter.yuvToRgb(
            this,
            converted
        )
        return converted
    }

    fun convert(cameraImage: ImageProxy): ConvertResult {
        val readyForProcessing = cameraImage.fromYuvToRgb()
            .compensateSensorRotation(cameraImage.imageInfo.rotationDegrees)
        displayBuffer.copyFrom(readyForProcessing)

        return ConvertResult(readyForProcessing, displayBuffer.bitmap())
    }
}

private class BitmapProxy {
    private var bitmap: Bitmap? = null
    private var canvas: Canvas? = null

    private fun bitmap(width: Int, height: Int): Bitmap {
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        } else {
            if (bitmap().width != width || bitmap().height != height) {
                throw IllegalStateException("bitmap().width != width || bitmap().height != height")
            }
        }
        return bitmap!!
    }

    fun bitmap() = bitmap!!

    fun bitmapLike(image: ImageProxy) = bitmap(image.width, image.height)
    fun bitmapLike(image: Bitmap) = bitmap(image.width, image.height)

    fun copyFrom(src: Bitmap): BitmapProxy {
        val dst = bitmapLike(src)
        if (canvas == null) {
            canvas = Canvas(dst)
        }
        canvas!!.drawBitmap(src, 0f, 0f, null)
        return this
    }
}