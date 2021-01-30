package com.tavrida.electro_counters.drawing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.tavrida.counter_scanner.scanning.NonblockingCounterReadingScanner
import com.tavrida.utils.PaintFontSizeManager
import com.tavrida.utils.toRectF

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
    }

    fun draw(
        inputBitmap: Bitmap,
        scanResult: NonblockingCounterReadingScanner.ScanResult
    ): Bitmap {
        val canvas = Canvas(inputBitmap)
        for (d in scanResult.digitsAtBoxes) {
            val box = d.box.toRectF()
            canvas.drawRect(box, digitsBoxPaint)

            val text = d.digit.toString()
            digitPaintManager.setTextSizeForHeight(box.height() - 4, text)
            canvas.drawText(text, box.left + 2, box.top - 2, digitPaintManager.paint)
        }
        return inputBitmap
    }
}