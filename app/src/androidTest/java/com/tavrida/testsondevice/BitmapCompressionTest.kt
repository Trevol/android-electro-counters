package com.tavrida.testsondevice

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tavrida.utils.*
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import java.io.*
import java.nio.ByteBuffer
import kotlin.system.measureTimeMillis


@RunWith(AndroidJUnit4::class)
class BitmapCompressionTest {
    companion object {
        init {
            OpenCVLoader.initDebug()
        }

        const val TAG = "BitmapCompressionTest_TESTS"

        fun appContext(): Context {
            return InstrumentationRegistry.getInstrumentation().targetContext
        }

        fun String.log() = Log.d(TAG, this)
        fun Any.log() = this.toString().log()

        // const val testFrameFileName = "test_for_compression.jpg"
        const val testFrameFileName = "00000.jpg"

        fun testBmp() = appContext().assets.open(testFrameFileName).use {
            BitmapFactory.decodeStream(it)
        }


        fun Bitmap.compressToArray(format: Bitmap.CompressFormat, quality: Int) =
            ByteArrayOutputStream().use {
                compress(format, quality, it)
                it.toByteArray()
            }

        fun Mat.toBitmap(): Bitmap {
            return Bitmap.createBitmap(cols(), rows(), Bitmap.Config.ARGB_8888)
                .apply { Utils.matToBitmap(this@toBitmap, this) }
        }

        //******************************************

    }

    @Test
    fun compressBitmapTest() {
        val originalBmp = testBmp()

        val originalPixels = originalBmp.pixelsBuffer()
        originalPixels.capacity().log()
        "------------------------------".log()

        val compressionConfigs = listOf(
            Bitmap.CompressFormat.JPEG to 100,
            Bitmap.CompressFormat.JPEG to 80,
            Bitmap.CompressFormat.JPEG to 50,
            Bitmap.CompressFormat.PNG to 100,
            Bitmap.CompressFormat.PNG to 50,
            Bitmap.CompressFormat.PNG to 20,
            Bitmap.CompressFormat.WEBP to 100,
            Bitmap.CompressFormat.WEBP to 99,
            Bitmap.CompressFormat.WEBP to 90,
            Bitmap.CompressFormat.WEBP to 80,
            Bitmap.CompressFormat.WEBP to 50,
        )

        for ((format, quality) in compressionConfigs) {
            val compressed = originalBmp.compressToArray(format, quality)

            val restoredBmp = BitmapFactory.decodeByteArray(compressed, 0, compressed.size)
            val restoredPixels = restoredBmp.pixelsBuffer()

            "compress: $format, $quality%".log()
            compressed.size.log()
            (originalPixels.array().contentEquals(restoredPixels.array())).log()
            "------------------------------".log()
        }

    }

    @Test
    fun measureCompression() {
        val originalBmp = testBmp()

        val compressionConfigs = listOf(
            Bitmap.CompressFormat.JPEG to 100,
            Bitmap.CompressFormat.JPEG to 75,
            Bitmap.CompressFormat.JPEG to 50,
            Bitmap.CompressFormat.PNG to 100,
            Bitmap.CompressFormat.WEBP to 100,
            Bitmap.CompressFormat.WEBP to 99,
            Bitmap.CompressFormat.WEBP to 90,
            Bitmap.CompressFormat.WEBP to 80,
            Bitmap.CompressFormat.WEBP to 50,
        )
        val n = 20
        //warmup
        for ((format, quality) in compressionConfigs) {
            val compressed = originalBmp.compressToArray(format, quality)
        }

        for ((format, quality) in compressionConfigs) {
            val millis = (1..n).map {
                measureTimeMillis {
                    val compressed = originalBmp.compressToArray(format, quality)
                }
            }
            "$format-$quality: ${millis.average()}".log()
        }
    }


    @Test
    fun writeToFile() {
        // val size = 1_228_800
        val size = 1_600_800
        val data = ByteArray(size) { 123 }

        var times = (0..9).map { i ->
            measureTimeMillis {
                val f = File(appContext().filesDir, "$i.data")
                FileOutputStream(f).use {
                    it.write(data)
                    it.flush()
                }
            }
        }
        times.average().log()
    }

    @Test
    fun write_read_raw_inMemory() {
        val bmp = testBmp()

        val buffer = ByteArrayOutputStream().use {
            bmp.toRawPixelData(it)
            it.toByteArray()
        }

        val mat = ByteArrayInputStream(buffer).use {
            it.rawPixelDataToMat()
        }

        Assert.assertArrayEquals(
            mat.toBitmap().pixelsBuffer().array(),
            bmp.pixelsBuffer().array()
        )
    }

    @Test
    fun write_read_raw_inFile() {
        val bmp = testBmp()

        val file = File(appContext().filesDir, "test.pixel_data")

        bmp.saveAsRawPixelData(file)
        val mat = imreadAsRawPixelData(file)

        Assert.assertEquals(bmp.width, mat.cols())
        Assert.assertEquals(bmp.height, mat.rows())
        Assert.assertArrayEquals(
            mat.toBitmap().pixelsBuffer().array(),
            bmp.pixelsBuffer().array()
        )
    }
}