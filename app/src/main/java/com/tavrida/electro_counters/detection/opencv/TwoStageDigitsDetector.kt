package com.tavrida.ElectroCounters.detection

import com.tavrida.utils.*
import org.opencv.core.Mat

class TwoStageDigitsDetector(
    val screenDetector: DarknetDetector,
    val digitsDetector: DarknetDetector
) {
    fun detect(image: Mat): TwoStageDetectionResult? {
        val screenDetection = screenDetector.detect(image).detections
            .filter { it.classId == screenClassId }
            // if multiple screen detected - choose closest to image center
            .minBy { it.box.center().L2squared(image.center()) }
            ?: return null

        val (screenImg, screenRoi) = image.roi(screenDetection.box.toRect(), .15, .15)
        val digitsDetections = digitsDetector.detect(screenImg).detections
        // return screenLocation: RectF, screenImage: Bitmap, digitsDetections (inside screen image)
        return TwoStageDetectionResult(
            screenRoi.toRectF(),
            screenImg.toBitmap(),
            digitsDetections
        )
    }

    companion object {
        const val screenClassId = 1
    }
}


