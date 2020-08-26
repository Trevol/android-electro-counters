package com.tavrida.ElectroCounters.detection

import android.content.Context
import com.tavrida.utils.Asset
import org.opencv.android.OpenCVLoader

class TwoStageDigitsDetectorProvider(context: Context) {
    val detector by lazy { instance.readDetector(context) }

    fun ensureDetector() {
        val d = detector
    }

    private object instance {
        private lateinit var _detector: TwoStageDigitsDetector
        private fun createDetector(context: Context): TwoStageDigitsDetector {
            val screenCfgFile = Asset.getFilePath(context, screenModelCfg, true)
            val screenModel = Asset.getFilePath(context, screenModelWeights, true)
            val screenDetector = DarknetDetector(screenCfgFile, screenModel, 320)

            val digitsCfgFile = Asset.getFilePath(context, digitsModelCfg, true)
            val digitsModel = Asset.getFilePath(context, digitsModelWeights, true)
            val digitsDetector = DarknetDetector(digitsCfgFile, digitsModel, 320)

            val detector = TwoStageDigitsDetector(
                screenDetector, digitsDetector,
                Asset.fileInDownloads(storageDir)
            )
            return detector
        }

        fun readDetector(context: Context): TwoStageDigitsDetector {
            if (!::_detector.isInitialized)
                _detector = createDetector(context)
            return _detector
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
    }
}