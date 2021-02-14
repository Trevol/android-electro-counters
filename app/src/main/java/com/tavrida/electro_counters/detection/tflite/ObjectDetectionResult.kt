package com.tavrida.electro_counters.detection.tflite

import android.graphics.RectF
import kotlinx.serialization.Serializable

@Serializable
data class ObjectDetectionResult(val location: RectF, val classId: Int, val score: Float)