package com.tavrida.electro_counters.drawing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.tavrida.counter_scanner.scanning.NonblockingCounterReadingScanner
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
        private const val BARCODE_MARK_R = 10f
    }

    fun draw(
        inputBitmap: Bitmap,
        scanResult: NonblockingCounterReadingScanner.ScanResult
    ): Bitmap {
        val canvas = Canvas(inputBitmap)
        for (d in scanResult.digitsAtLocations) {
            canvas.drawRect(d.location, digitsBoxPaint)

            val text = d.digit.toString()
            digitPaintManager.setTextSizeForHeight(d.location.height() - 4, text)
            canvas.drawText(text, d.location.left + 2, d.location.top - 2, digitPaintManager.paint)
        }
        for (barcode in scanResult.barcodes) {
            val box = barcode.boundingBox!!
            canvas.drawCircle(
                box.exactCenterX(),
                box.exactCenterY(),
                BARCODE_MARK_R,
                barckodeMarkPaint
            )
        }
        return inputBitmap
    }
}