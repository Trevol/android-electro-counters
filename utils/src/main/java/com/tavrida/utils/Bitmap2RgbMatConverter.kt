package com.tavrida.utils

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

class Bitmap2RgbMatConverter {
    private val rgbaBuffer = Mat()

    fun convert(image: Bitmap): Mat {
        Utils.bitmapToMat(image, rgbaBuffer, true)
        return Mat().apply {
            Imgproc.cvtColor(rgbaBuffer, this, Imgproc.COLOR_RGBA2RGB)
        }
    }
}