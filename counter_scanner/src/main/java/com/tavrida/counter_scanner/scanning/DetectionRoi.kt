package com.tavrida.counter_scanner.scanning

import android.graphics.*
import android.util.Size
import com.tavrida.utils.tl

class DetectionRoi(val size: Size) {
    private var imageSize: Size? = null
    private var roiRect: Rect? = null

    data class ExtractImageResult(val roiImg: Bitmap, val roiRect: Rect, val roiOrigin: Point)

    fun extractImage(inputImg: Bitmap): ExtractImageResult {
        val r = roiRect(inputImg)
        return ExtractImageResult(
            Bitmap.createBitmap(inputImg, r.left, r.top, r.width(), r.height()),
            r,
            r.tl()
        )
    }

    inline fun roiRect(src: Bitmap): Rect {
        val centerX = src.width / 2.0f
        val centerY = src.height / 2.0f

        val halfW = size.width / 2f
        val halfH = size.height / 2f
        return Rect(
            (centerX - halfW).toInt(),
            (centerY - halfH).toInt(),
            (centerX + halfW).toInt(),
            (centerY + halfH).toInt()
        )
    }

    fun draw(img: Bitmap, roiPaint: Paint): Bitmap {
        val r = roiRect(img)
        Canvas(img).drawRect(r, roiPaint)
        return img
    }
}


private object roi {
    val w: Int = 400
    val h: Int = 180

    val roiPaint = Paint().apply {
        color = Color.rgb(0, 255, 0)
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    fun roiBitmap(src: Bitmap): Pair<Bitmap, Rect> {
        val r = rect(src)
        return Bitmap.createBitmap(src, r.left, r.top, r.width(), r.height()) to r
    }

    inline fun rect(src: Bitmap): Rect {
        val centerX = src.width / 2.0f
        val centerY = src.height / 2.0f

        val halfW = w / 2f
        val halfH = h / 2f
        return Rect(
            (centerX - halfW).toInt(),
            (centerY - halfH).toInt(),
            (centerX + halfW).toInt(),
            (centerY + halfH).toInt()
        )
    }

    fun draw(img: Bitmap): Bitmap {
        val r = rect(img)
        Canvas(img).drawRect(r, roiPaint)
        return img
    }
}