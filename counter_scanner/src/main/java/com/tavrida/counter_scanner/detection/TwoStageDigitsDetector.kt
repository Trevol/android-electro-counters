package com.tavrida.counter_scanner.detection

import com.tavrida.counter_scanner.utils.*
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Rect2d

class TwoStageDigitsDetector(
    val screenDetector: DarknetDetector,
    val digitsDetector: DarknetDetector
) {
    fun detect(image: Mat): TwoStageDigitDetectionResult? {
        val screenDetection = screenDetector.detect(image).detections
            .filter { it.classId == screenClassId }
            .minBy { it.box.center().L2squared(image.center()) } // choose closest to image center
            ?: return null

        val (screenImg, screenRoi) = image.roi(screenDetection.box.toRect(), .15, .15)
        val digitsDetections = digitsDetector.detect(screenImg).detections
            .map { DigitDetectionResult(it.classId, it.classScore, it.box.remap(screenRoi)) }

        return TwoStageDigitDetectionResult(
            null,
            null,
            screenRoi.toRect2d(),
            screenDetection.classScore,
            digitsDetections
        )
    }

    companion object {
        const val screenClassId = 1

        private fun Rect2d.remap(boxOfBox: Rect) = Rect2d(
            x + boxOfBox.x,
            y + boxOfBox.y,
            width,
            height
        )
    }
}





