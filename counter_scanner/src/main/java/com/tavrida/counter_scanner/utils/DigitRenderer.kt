package com.tavrida.counter_scanner.utils

import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

class DigitRenderer(fontHeight: Int, private val fontFace: Int, private val fontThickness: Int = 1) {
    class Vector2d(val x: Double, val y: Double)

    private val fontScale = getFontScale(referenceChar, fontHeight, fontFace, fontThickness)
    private val displacementToOrd = Imgproc.getTextSize(referenceChar, fontFace, fontScale, fontThickness, null)
        .let { textSize -> Vector2d(-0.5 * textSize.width, 0.5 * textSize.height) }

    fun render(img: Mat, digit: Int, center: Point, color: Scalar) {
        assert(digit in 0..9)
        Imgproc.putText(img, digit.toString(), textOrd(center), fontFace, fontScale, color, fontThickness)
    }

    private fun textOrd(center: Point) = Point(center.x + displacementToOrd.x, center.y + displacementToOrd.y)

    companion object {
        const val referenceChar = "1"

        fun getFontScale(text: String, desiredHeight: Int, fontFace: Int, thickness: Int): Double {
            val startingFontScale = 40.0
            val startingFontSize = Imgproc.getTextSize(text, fontFace, startingFontScale, thickness, null)
            val desiredScale = desiredHeight * startingFontScale / startingFontSize.height
            return desiredScale
        }

    }
}