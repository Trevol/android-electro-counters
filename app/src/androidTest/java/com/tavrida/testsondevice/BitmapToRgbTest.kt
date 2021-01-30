package com.tavrida.testsondevice

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tavrida.testsondevice.BitmapToRgbTest.Companion.log
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
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

    @Test
    fun rgbaToGrayscaleTest() {
        {
            val rgba = Mat(1, 1, CvType.CV_8UC4, Scalar(10.0, 20.0, 10.0, 127.0))
            rgba.get(0, 0).toList().log()
            val grayFromRgba = Mat().apply { Imgproc.cvtColor(rgba, this, Imgproc.COLOR_RGBA2GRAY) }
            grayFromRgba.get(0, 0).toList().log()
        }();

        {
            val rgb = Mat(1, 1, CvType.CV_8UC3, Scalar(10.0, 20.0, 10.0, 127.0))
            rgb.get(0, 0).toList().log()
            val grayFromRgb = Mat().apply { Imgproc.cvtColor(rgb, this, Imgproc.COLOR_RGBA2GRAY) }
            grayFromRgb.get(0, 0).toList().log()
        }()

        "-----------------------".log();

        {
            val rgba = Mat(1, 1, CvType.CV_8UC4, Scalar(10.0, 20.0, 10.0, 127.0))
            rgba.get(0, 0).toList().log()
            val mRgba = Mat().apply { Imgproc.cvtColor(rgba, this, Imgproc.COLOR_RGBA2mRGBA) }
            mRgba.get(0, 0).toList().log()
            val grayFromMrgba = Mat().apply { Imgproc.cvtColor(mRgba, this, Imgproc.COLOR_RGBA2GRAY) }
            grayFromMrgba.get(0, 0).toList().log()
        }()
    }
}