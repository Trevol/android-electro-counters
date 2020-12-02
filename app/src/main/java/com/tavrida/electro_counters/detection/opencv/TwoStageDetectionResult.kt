package com.tavrida.ElectroCounters.detection

import org.opencv.core.Mat
import org.opencv.core.Rect

data class TwoStageDetectionResult(
    val screenLocation: Rect,
    val screenImage: Mat,
    val digitsDetections: Collection<ObjectDetectionResult>
)