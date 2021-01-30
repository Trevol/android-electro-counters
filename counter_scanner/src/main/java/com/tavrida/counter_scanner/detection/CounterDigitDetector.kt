package com.tavrida.counter_scanner.detection

import android.graphics.RectF
import com.tavrida.electro_counters.detection.tflite.new_detector.TfliteDetector
import org.opencv.core.Rect2d

class CounterDigitDetector(detector: TfliteDetector) {
    fun detect(): CounterDigitDetectionResult = TODO()
}

data class CounterDigitDetectionResult(
    val screenLocation: RectF,
    val screenScore: Float,
    val digitsDetections: List<DigitDetectionResult>
)

data class DigitDetectionResult(val digit: Int, val score: Float, val location: RectF)
