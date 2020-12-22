package com.tavrida.electro_counters.counter_scanner

import android.content.Context
import com.tavrida.counter_scanner.detection.DarknetDetector
import com.tavrida.counter_scanner.detection.TwoStageDigitsDetector
import com.tavrida.counter_scanner.scanning.nonblocking.NonblockingCounterReadingScanner
import com.tavrida.utils.Asset
import org.opencv.android.OpenCVLoader
import org.opencv.core.CvType
import org.opencv.core.Mat

class CounterScannerProvider(context: Context) {
    private val detectorProvider = TwoStageDigitsDetectorProvider(context)

    init {
        detectorProvider.init()
    }

    fun counterScanner() = NonblockingCounterReadingScanner(detectorProvider.detector)

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
            private const val digitsModelWeights = "yolov3-tiny-10cls-digits-320.7.weights"

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
}