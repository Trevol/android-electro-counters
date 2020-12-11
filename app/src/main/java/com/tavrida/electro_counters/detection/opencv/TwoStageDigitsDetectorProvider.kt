package com.tavrida.ElectroCounters.detection

import android.content.Context
import android.util.Log
import com.tavrida.utils.Asset
import org.opencv.android.OpenCVLoader

class TwoStageDigitsDetectorProvider(context: Context) {
    val detector by lazy { instances.readDetector(context) }

    fun ensureDetector() {
        val d = detector
    }

    object instances {
        lateinit var screenDetector: DarknetDetector
        lateinit var digitsDetector: DarknetDetector

        fun readDetector(context: Context): TwoStageDigitsDetector {
            if (!::screenDetector.isInitialized) {
                screenDetector = createDarknetDetector(context, screenModelCfg, screenModelWeights)
                digitsDetector = createDarknetDetector(context, digitsModelCfg, digitsModelWeights)
            }
            return TwoStageDigitsDetector(screenDetector, digitsDetector)
        }
    }

    companion object {
        init {
            OpenCVLoader.initDebug()
        }

        private const val screenModelCfg = "yolov3-tiny-2cls-320.cfg"
        private const val screenModelWeights = "yolov3-tiny-2cls-320.weights"
        private const val digitsModelCfg = "yolov3-tiny-10cls-320.cfg"
        private const val digitsModelWeights = "yolov3-tiny-10cls-320.4.weights"

        private const val storageDir: String = "ElectroCounters"

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