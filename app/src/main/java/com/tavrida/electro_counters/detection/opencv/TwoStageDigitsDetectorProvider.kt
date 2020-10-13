/*
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

    private object instances {
        private lateinit var screenDetector: DarknetDetector
        private lateinit var digitsDetector: DarknetDetector

        fun readDetector(context: Context): TwoStageDigitsDetector {
            if (!::screenDetector.isInitialized) {
                screenDetector = createDarknetDetector(context, screenModelCfg, screenModelWeights)
                digitsDetector = createDarknetDetector(context, digitsModelCfg, digitsModelWeights)
            }
            return TwoStageDigitsDetector(
                screenDetector, digitsDetector,
                context,
                null
            )
        }
    }

    companion object {
        init {
            OpenCVLoader.initDebug()
        }
    }
}*/
