package com.tavrida.ElectroCounters.detection

import android.graphics.Bitmap
import android.graphics.RectF

data class TwoStageDetectionResult(
    val screenLocation: RectF,
    val screenImage: Bitmap,
    val digitsDetections: Collection<ObjectDetectionResult>
)