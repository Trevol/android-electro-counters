@file:UseSerializers(RectFSerializer::class)
package com.tavrida.counter_scanner.detection

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.RectF
import com.tavrida.electro_counters.detection.tflite.new_detector.TfliteDetector
import com.tavrida.utils.serialization.RectFSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

class ScreenDigitDetector(private val objectDetector: TfliteDetector) {
    fun detect(img: Bitmap, imgOrigin: Point?): ScreenDigitDetectionResult {
        val objectDetections = objectDetector
            .detect(img, SCORE_THRESHOLD)
            .remapToFullImage(imgOrigin)
        val screenDetection = objectDetections.firstOrNull { it.isScreen }
        val digitDetections = objectDetections.filter { it.isDigit }
        return ScreenDigitDetectionResult(
            screenDetection = screenDetection?.let { ScreenDetectionResult(it.score, it.location) },
            digitsDetections = digitDetections.map {
                DigitDetectionResult(
                    digit = it.classId,
                    it.score,
                    it.location
                )
            }
        )
    }

    private companion object {
        const val SCORE_THRESHOLD = .2f
        const val screenClass = 10
        inline val ObjectDetectionResult.isScreen get() = classId == screenClass
        inline val ObjectDetectionResult.isDigit get() = !isScreen

        private fun List<ObjectDetectionResult>.remapToFullImage(detectionOrigin: Point?) =
            if (detectionOrigin == null)
                this
            else
                this.map {
                    ObjectDetectionResult(
                        it.classId,
                        it.score,
                        it.location.remap(detectionOrigin)
                    )
                }

        private inline fun RectF.remap(origin: Point) =
            RectF(left + origin.x, top + origin.y, right + origin.x, bottom + origin.y)

    }
}

@Serializable
data class ScreenDigitDetectionResult(
    val screenDetection: ScreenDetectionResult?,
    val digitsDetections: List<DigitDetectionResult>
)
@Serializable
data class ScreenDetectionResult(val score: Float, val location: RectF)
@Serializable
data class DigitDetectionResult(val digit: Int, val score: Float, val location: RectF)
