package com.tavrida.electro_counters.counter_scanner

import android.content.Context
import com.tavrida.counter_scanner.CounterReadingScanner
import com.tavrida.counter_scanner.detection.DarknetDetector
import com.tavrida.counter_scanner.detection.TwoStageDigitsDetector
import com.tavrida.utils.Asset
import org.opencv.android.OpenCVLoader

class CounterScannerProvider(context: Context) {
    private val detectorProvider = TwoStageDigitsDetectorProvider(context)

    init {
        detectorProvider.init()
    }

    fun counterScanner() = CounterReadingScanner(detectorProvider.detector)

    companion object {
        init {
            OpenCVLoader.initDebug()
        }
    }

    class TwoStageDigitsDetectorProvider(context: Context) {
        val detector by lazy { instances.readDetector(context) }

        fun init() {
            val d = detector
        }

        object instances {
            lateinit var screenDetector: DarknetDetector
            lateinit var digitsDetector: DarknetDetector

            fun readDetector(context: Context): TwoStageDigitsDetector {
                if (!instances::screenDetector.isInitialized) {
                    screenDetector =
                        createDarknetDetector(context, screenModelCfg, screenModelWeights)
                    digitsDetector =
                        createDarknetDetector(context, digitsModelCfg, digitsModelWeights)
                }
                return TwoStageDigitsDetector(screenDetector, digitsDetector)
            }
        }

        companion object {
            private const val screenModelCfg = "yolov3-tiny-2cls-320.cfg"
            private const val screenModelWeights = "yolov3-tiny-2cls-320.weights"
            private const val digitsModelCfg = "yolov3-tiny-10cls-320.cfg"
            private const val digitsModelWeights = "yolov3-tiny-10cls-320.4.weights"

            private fun createDarknetDetector(
                context: Context,
                modelCfg: String,
                modelWeights: String
            ): DarknetDetector {
                val screenCfgFile = Asset.getFilePath(context, modelCfg, true)
                val screenModel = Asset.getFilePath(context, modelWeights, true)
                return DarknetDetector(screenCfgFile, screenModel, 320)
            }
        }
    }
}