package com.tavrida.utils

import android.graphics.Paint
import android.graphics.Rect

class PaintFontSizeManager(val paint: Paint) {
    private val refBounds = Rect()

    init {
        precalcReferenceSize()
    }

    private fun precalcReferenceSize() {
        paint.textSize = refSize
        paint.getTextBounds(refText, 0, refText.length, refBounds)

    }

    fun setTextSizeForWidth(desiredWidth: Float, text: String) {
        // got here https://stackoverflow.com/a/21895626
        val desiredTextSize: Float = refSize * desiredWidth / refBounds.width()
        paint.textSize = desiredTextSize
    }

    fun setTextSizeForHeight(desiredHeight: Float, text: String) {
        // got here https://stackoverflow.com/a/21895626
        val desiredTextSize: Float = refSize * desiredHeight / refBounds.height()
        paint.textSize = desiredTextSize
    }

    private companion object {
        private const val refSize = 50f
        private const val refText = "0"
    }
}