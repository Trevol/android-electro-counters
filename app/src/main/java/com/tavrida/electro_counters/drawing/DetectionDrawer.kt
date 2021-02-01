package com.tavrida.electro_counters.drawing

/*
import android.graphics.*
import com.tavrida.counter_scanner.detection.TwoStageDigitDetectionResult
import com.tavrida.utils.PaintFontSizeManager
import com.tavrida.utils.toRectF

class DetectionDrawer {
    val screenPaint = Paint().apply {
        color = Color.argb(255, 255, 0, 0)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    val digitsBoxPaint = Paint().apply {
        color = Color.argb(255, 0, 255, 0)
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    val digitPaint = Paint().apply {
        color = Color.argb(255, 255, 0, 0)
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 1f
        textSize = 24f
    }

    private val digitPainSizeSetter = PaintFontSizeManager(digitPaint)

    val gray = Color.argb(255, 127, 127, 127)

    data class DrawResult(
        val inputBitmapWithDrawing: Bitmap,
        val screenImageWithDrawing: Bitmap,
        val digitsDetectionBitmap: Bitmap
    )

    fun drawDetectionResults(
        inputImage: Bitmap,
        screenImage: Bitmap,
        detectionResult: TwoStageDigitDetectionResult
    ): DrawResult {

        Canvas(inputImage).drawRect(detectionResult.screenBox.toRectF(), screenPaint)

        val digitsDetectionBitmap = Bitmap.createBitmap(
            screenImage.width,
            screenImage.height,
            Bitmap.Config.ARGB_8888
        ).apply { eraseColor(gray) }

        val screenImageCanvas = Canvas(screenImage)
        */
/*val digitsImageCanvas = Canvas(digitsDetectionBitmap)
        for (d in detectionResult.digitsDetections) {
            val boxF = d.boxInScreen.toRectF()
            screenImageCanvas.drawRect(boxF, digitsBoxPaint)
            digitsImageCanvas.drawRect(boxF, digitsBoxPaint)

            val text = d.digit.toString()
            //calc and set fontSize to fit in box
            digitPainSizeSetter.setTextSizeForHeight(boxF.height() - 4, text)
            digitsImageCanvas.drawText(
                text,
                boxF.left + 2,
                boxF.bottom - 2,
                digitPaint
            )
        }*//*

        return DrawResult(inputImage, screenImage, digitsDetectionBitmap)
    }
}*/
