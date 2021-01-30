package com.tavrida.counter_scanner.detection

import android.graphics.RectF

data class ObjectDetectionResult(
    val classId: Int,
    val score: Float,
    val location: RectF
)