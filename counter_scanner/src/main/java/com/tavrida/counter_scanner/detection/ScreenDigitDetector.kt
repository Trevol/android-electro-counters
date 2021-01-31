package com.tavrida.counter_scanner.detection

import android.graphics.Bitmap
import android.graphics.RectF
import com.tavrida.electro_counters.detection.tflite.new_detector.TfliteDetector

class ScreenDigitDetector(private val objectDetector: TfliteDetector) {
    fun detect(img: Bitmap): ScreenDigitDetectionResult {
        val objectDetections = objectDetector.detect(img, SCORE_THRESHOLD)
        val screenDetection = objectDetections.firstOrNull { it.isScreen }
        val digitDetections = objectDetections.filter { it.isDigit }
        return ScreenDigitDetectionResult(
            screenDetection = screenDetection?.let { ScreenDetectionResult(it.score, it.location) },
            digitsDetections = digitDetections.map {
                DigitDetectionResult(
                    digit = it.classId,
                    it.score,
                    it.location
                )
            }
        )
    }

    private companion object {
        const val SCORE_THRESHOLD = .2f
        const val screenClass = 10
        inline val ObjectDetectionResult.isScreen get() = classId == screenClass
        inline val ObjectDetectionResult.isDigit get() = !isScreen
    }
}

data class ScreenDigitDetectionResult(
    val screenDetection: ScreenDetectionResult?,
    val digitsDetections: List<DigitDetectionResult>
)

data class ScreenDetectionResult(val score: Float, val location: RectF)
data class DigitDetectionResult(val digit: Int, val score: Float, val location: RectF)
