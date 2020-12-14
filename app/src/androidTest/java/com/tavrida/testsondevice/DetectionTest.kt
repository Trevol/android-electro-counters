package com.tavrida.testsondevice

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tavrida.counter_scanner.detection.DarknetDetector
import com.tavrida.counter_scanner.detection.TwoStageDigitsDetector
import com.tavrida.counter_scanner.utils.bgr2rgb
import com.tavrida.electro_counters.counter_scanner.CounterScannerProvider
import com.tavrida.utils.Asset
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs


@RunWith(AndroidJUnit4::class)
class DetectionTest {
    companion object {
        init {
            OpenCVLoader.initDebug()
        }

        const val TAG = "DET_TESTS"
        const val frameName = "20201009165842_0000.jpg"

        fun appContext(): Context {
            return InstrumentationRegistry.getInstrumentation().targetContext
        }

        fun getScreenDetector(context: Context): DarknetDetector {
            CounterScannerProvider.TwoStageDigitsDetectorProvider(context).init()
            return CounterScannerProvider.TwoStageDigitsDetectorProvider.instances.screenDetector
        }

        fun getTwoStageDetector(context: Context): TwoStageDigitsDetector {
            val provider = CounterScannerProvider.TwoStageDigitsDetectorProvider(context)
            provider.init()
            return provider.detector
        }

        fun readFrame(context: Context): Mat {
            val frame = Imgcodecs.imread(
                Asset.getFilePath(context, frameName, false)
            )
            return frame
        }

    }

    @Test
    fun detectionTest() {
        val appContext = appContext()

        val testFrame = readFrame(appContext).bgr2rgb()

        val twoStageDetector = getTwoStageDetector(appContext)
        val result = twoStageDetector.detect(testFrame)
    }
}