package com.tavrida.electro_counters.detection.tflite

import android.graphics.RectF

data class ObjectPrediction(val location: RectF, val label: String, val score: Float)