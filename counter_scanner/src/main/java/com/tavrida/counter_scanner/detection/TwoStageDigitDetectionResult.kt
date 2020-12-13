package com.tavrida.counter_scanner.detection

import org.opencv.core.Rect2d

data class TwoStageDigitDetectionResult(
    val counterBox: Rect2d?,
    val counterScore: Float?,
    val screenBox: Rect2d,
    val screenScore: Float,

    val digitsDetections: List<DigitDetectionResult>
)

data class DigitDetectionResult(val digit: Int, val score: Float, val boxInScreen: Rect2d, val boxInImage: Rect2d)