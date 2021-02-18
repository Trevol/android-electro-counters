package com.tavrida.electro_counters.counter_scanner

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import com.tavrida.counter_scanner.detection.ScreenDigitDetector
import com.tavrida.counter_scanner.scanning.DetectionRoi
import com.tavrida.counter_scanner.scanning.CounterScanner
import com.tavrida.electro_counters.TelemetryRecorder
import com.tavrida.electro_counters.detection.tflite.new_detector.TfliteDetector
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel


class CounterScannerProvider2() {
    fun createScanner(
        context: Context,
        detectorRoi: DetectionRoi,
        telemetryRecorder: TelemetryRecorder
    ) =
        CounterScanner(createDetector(context), detectorRoi, 500, telemetryRecorder)

    private fun createDetector(context: Context) =
        ScreenDigitDetector(objectDetector.instance(context))

    private object objectDetector {
        private const val MODEL_FILE = "screen_digits_320x128_251.tflite"
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
            if (warmup) {
                instance.detect(
                    Bitmap.createBitmap(inputSize.width, inputSize.height, Bitmap.Config.ARGB_8888),
                    .2f
                )
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
}