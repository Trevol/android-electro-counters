package com.tavrida.electro_counters

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class ObjectPredictionsView : View {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) :
            super(context, attrs, defStyle)

    val paint = Paint().apply {
        color = Color.argb(255, 255, 0, 0)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    var locations: List<RectF> = listOf()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (loc in locations) {
            canvas.drawRect(loc, paint)
        }
    }

    fun showLocations(locations: List<RectF>) {
        this.locations = locations
        invalidate()
    }


    companion object {

    }
}
