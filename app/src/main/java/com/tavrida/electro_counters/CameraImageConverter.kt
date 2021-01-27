package com.tavrida.electro_counters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.camera.core.ImageProxy
import com.tavrida.utils.camera.YuvToRgbConverter2

class CameraImageConverter(context: Context) {
    private val forProcessing = BitmapProxy()
    private var forDisplay = BitmapProxy()
    private val yuvToRgbConverter = YuvToRgbConverter2(context)

    data class ConvertResult(val processing: Bitmap, val display: Bitmap)

    fun convert(cameraImage: ImageProxy): ConvertResult {
        // cameraImage => processingBitmap
        val processing = forProcessing.bitmapLike(cameraImage)
        yuvToRgbConverter.yuvToRgb(
            cameraImage,
            processing
        )
        // processingBitmap => displayBitmap
        val display = forProcessing.copyTo(forDisplay)
        return ConvertResult(processing, display)
    }
}

private class BitmapProxy {
    private var bitmap: Bitmap? = null
    private var canvas: Canvas? = null

    private fun bitmap(width: Int, height: Int): Bitmap {
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }
        return bitmap!!
    }


    fun bitmapLike(image: ImageProxy) = bitmap(image.width, image.height)

    fun copyTo(dst: BitmapProxy): Bitmap {
        val src = bitmap!!
        if (canvas == null) {
            canvas = Canvas(dst.bitmap(src.width, src.height))
        }
        canvas!!.drawBitmap(src, 0f, 0f, null)
        return bitmap!!
    }
}