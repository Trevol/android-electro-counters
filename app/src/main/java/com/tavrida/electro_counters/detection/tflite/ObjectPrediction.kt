package com.tavrida.electro_counters.detection.tflite

import android.graphics.RectF

data class ObjectPrediction(val location: RectF, val labelId: Int, val label: String, val score: Float)