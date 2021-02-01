package com.tavrida.electro_counters.counter_scanner

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import com.tavrida.counter_scanner.detection.ScreenDigitDetector
import com.tavrida.counter_scanner.scanning.NonblockingCounterReadingScanner
import com.tavrida.electro_counters.detection.tflite.new_detector.TfliteDetector
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

class CounterScannerProvider2() {
    fun counterScanner(context: Context) =
        NonblockingCounterReadingScanner(createDetector(context), 500)

    private fun createDetector(context: Context) =
        ScreenDigitDetector(objectDetector.instance(context))

    private object objectDetector {
        private const val MODEL_FILE = ""
        private val inputSize = Size(320, 128)
        private var instance: TfliteDetector? = null

        fun instance(context: Context): TfliteDetector {
            if (instance == null) {
                instance = createInstance(context, warmup = true)
            }
            return instance!!
        }

        private fun createInstance(context: Context, warmup: Boolean = true): TfliteDetector {
            val instance = mapAssetFile(context, MODEL_FILE)
                .let { TfliteDetector(it, inputSize.height, inputSize.width) }
            if (warmup){
                Bitmap.createBitmap(inputSize.width, inputSize.height, )
                instance.detect()
            }
            return instance
        }

        private fun mapAssetFile(context: Context, fileName: String): ByteBuffer {
            val assetFd = context.assets.openFd(fileName)
            val start = assetFd.startOffset
            val length = assetFd.declaredLength
            return FileInputStream(assetFd.fileDescriptor).channel
                .map(FileChannel.MapMode.READ_ONLY, start, length)
        }
    }

    /*
    class TwoStageDigitsDetectorProvider(context: Context) {
        val detector by lazy { instances.readDetector(context, warmup = true) }

        fun init() {
            val d = detector
        }

        object instances {
            lateinit var screenDetector: DarknetDetector
            lateinit var digitsDetector: DarknetDetector

            fun readDetector(context: Context, warmup: Boolean): TwoStageDigitsDetector {
                if (!instances::screenDetector.isInitialized) {
                    screenDetector =
                        createDarknetDetector(context, screenModelCfg, screenModelWeights, warmup)
                    digitsDetector =
                        createDarknetDetector(context, digitsModelCfg, digitsModelWeights, warmup)
                }
                return TwoStageDigitsDetector(screenDetector, digitsDetector)
            }
        }

        companion object {
            private const val screenModelCfg = "yolov3-tiny-2cls-320.cfg"
            private const val screenModelWeights = "yolov3-tiny-2cls-counter-screeen-320.4.weights"
            private const val digitsModelCfg = "yolov3-tiny-10cls-320.cfg"
            private const val digitsModelWeights = "yolov3-tiny-10cls-digits-320.7.1.weights"

            private fun createDarknetDetector(
                context: Context,
                modelCfg: String,
                modelWeights: String,
                warmup: Boolean
            ): DarknetDetector {
                val screenCfgFile = Asset.getFilePath(context, modelCfg, true)
                val screenModel = Asset.getFilePath(context, modelWeights, true)
                val darknetDetector = DarknetDetector(screenCfgFile, screenModel, 320)
                if (warmup) {
                    darknetDetector.detect(Mat(320, 320, CvType.CV_8UC3))
                }
                return darknetDetector
            }
        }
    }
    */
}