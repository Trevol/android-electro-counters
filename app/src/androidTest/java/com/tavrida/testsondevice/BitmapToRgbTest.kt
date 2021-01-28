package com.tavrida.testsondevice

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc


@RunWith(AndroidJUnit4::class)
class BitmapToRgbTest {
    companion object {
        init {
            OpenCVLoader.initDebug()
        }

        const val TAG = "BitmapToRgbTest_TESTS"

        fun appContext(): Context {
            return InstrumentationRegistry.getInstrumentation().targetContext
        }

        fun String.log() = Log.d(TAG, this)
        fun Any.log() = this.toString().log()
    }

    @Test
    fun bmpToRgb() {
        val bmp = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.argb(127, 255, 255, 255))

        val rgbMat = Mat()
        val rgbaMat = Mat()
        Utils.bitmapToMat(bmp, rgbaMat)
        Imgproc.cvtColor(rgbaMat, rgbMat, Imgproc.COLOR_RGBA2RGB)
        rgbaMat.get(0, 0).toList().log()
        rgbMat.get(0, 0).toList().log()


        Utils.bitmapToMat(bmp, rgbaMat, true)
        Imgproc.cvtColor(rgbaMat, rgbMat, Imgproc.COLOR_RGBA2RGB)
        rgbaMat.get(0, 0).toList().log()
        rgbMat.get(0, 0).toList().log()

    }
}