package com.tavrida.electro_counters

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import java.util.*
import kotlin.random.Random

class ObjectPredictionsView : View {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) :
            super(context, attrs, defStyle)

    val paint = Paint().apply {
        color = Color.argb(255, 255, 0, 0)
        style = Paint.Style.STROKE
        strokeWidth = 5.56f
    }

    lateinit var rect: Rect

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (this::rect.isInitialized) {
            canvas.drawRect(rect, paint)
        }
    }

    fun generateNewRect() {
        rect = randomRect()
        invalidate()
    }

    fun randomRect(): Rect {
        val rectWidth = width - paddingStart - paddingEnd
        val rectHeight = height - paddingTop - paddingBottom
        return randomRect(paddingStart, paddingTop, rectWidth, rectHeight)
    }

    companion object {
        fun RectWH(left: Int, top: Int, width: Int, height: Int) =
            Rect(left, top, left + width, top + height)

        fun randomRect(x: Int, y: Int, width: Int, height: Int): Rect {
            val x = Random.nextInt(x, x + width - 10)
            val y = Random.nextInt(y, y + height - 10)
            val width = Random.nextInt(10, width - x)
            val height = Random.nextInt(10, height - y)
            return RectWH(x, y, width, height)
        }
    }
}
