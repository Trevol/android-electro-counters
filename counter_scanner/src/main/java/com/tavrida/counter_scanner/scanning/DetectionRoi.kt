package com.tavrida.counter_scanner.scanning

import android.graphics.*
import android.util.Size
import com.tavrida.utils.assert
import com.tavrida.utils.copy
import com.tavrida.utils.size
import com.tavrida.utils.tl

class DetectionRoi(val size: Size) {
    private var imageSize: Size? = null
    private var roiRect: Rect? = null

    private fun setImageSize(imgSize: Size) {
        if (imageSize == null) {
            imageSize = imgSize
            return
        }
        //check: accept images (frames) of same size
        (imageSize == imgSize).assert("imageSize != imgSize")
    }

    data class ExtractImageResult(val roiImg: Bitmap, val roiRect: Rect, val roiOrigin: Point)

    fun extractImage(inputImg: Bitmap): ExtractImageResult {
        setImageSize(inputImg.size)
        val r = roiRect()
        return ExtractImageResult(
            Bitmap.createBitmap(inputImg, r.left, r.top, r.width(), r.height()),
            r,
            r.tl()
        )
    }

    private inline fun roiRect(): Rect {
        if (roiRect == null) {
            roiRect = calcRoiRect(imageSize!!, size)
        }
        return roiRect!!
    }

    fun draw(img: Bitmap, roiPaint: Paint): Bitmap {
        setImageSize(img.size)
        val r = roiRect()
        Canvas(img).drawRect(r, roiPaint)
        return img
    }

    private companion object {
        fun calcRoiRect(imageSize: Size, roiSize: Size): Rect {
            val centerX = imageSize.width / 2.0f
            val centerY = imageSize.height / 2.0f

            val halfW = roiSize.width / 2f
            val halfH = roiSize.height / 2f
            return Rect(
                (centerX - halfW).toInt(),
                (centerY - halfH).toInt(),
                (centerX + halfW).toInt(),
                (centerY + halfH).toInt()
            )
        }
    }
}