package com.tavrida.electro_counters.drawing

import android.graphics.*
import com.tavrida.counter_scanner.scanning.CounterScaningResult
import com.tavrida.utils.PaintFontSizeManager

class ScanResultDrawer {
    companion object {
        private val digitsBoxPaint = Paint().apply {
            color = Color.argb(255, 0, 255, 0)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        private val digitPaintManager = PaintFontSizeManager(
            Paint().apply {
                color = Color.argb(255, 255, 0, 0)
                style = Paint.Style.FILL_AND_STROKE
                strokeWidth = 1f
                textSize = 24f
            })

        private val barckodeMarkPaint = Paint().apply {
            color = Color.argb(200, 0, 255, 0)
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = 1f
        }
        private const val BARCODE_MARK_R = 10
    }

    fun draw(
        inputBitmap: Bitmap,
        scanResult: CounterScaningResult
    ): Bitmap {
        val canvas = Canvas(inputBitmap)
        for (d in scanResult.digitsAtLocations) {
            canvas.drawRect(d.location, digitsBoxPaint)

            val text = d.digit.toString()
            digitPaintManager.setTextSizeForHeight(d.location.height() - 4, text)
            canvas.drawText(text, d.location.left + 2, d.location.top - 2, digitPaintManager.paint)
        }

        scanResult.consumerInfo?.barcodeLocation?.let { location ->
            val markRect = Rect(
                location.centerX() - BARCODE_MARK_R, location.centerY() - BARCODE_MARK_R,
                location.centerX() + BARCODE_MARK_R, location.centerY() + BARCODE_MARK_R
            )
            canvas.drawRect(markRect, barckodeMarkPaint)
            /*canvas.drawCircle(
                location.exactCenterX(),
                location.exactCenterY(),
                BARCODE_MARK_R,
                barckodeMarkPaint
            )*/
        }

        return inputBitmap
    }
}