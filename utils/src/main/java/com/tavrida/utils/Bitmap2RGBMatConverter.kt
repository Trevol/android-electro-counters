package com.tavrida.utils

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

class Bitmap2RGBMatConverter {
    private val rgbaMatBuffer = Mat()
    private val rgbMatBuffer = Mat()

    fun convert(image: Bitmap): Mat {
        Utils.bitmapToMat(image, rgbaMatBuffer)
        Imgproc.cvtColor(rgbaMatBuffer, rgbMatBuffer, Imgproc.COLOR_RGBA2RGB)
        return rgbMatBuffer
    }
}