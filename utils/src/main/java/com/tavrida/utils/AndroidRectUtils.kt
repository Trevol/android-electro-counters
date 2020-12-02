package com.tavrida.utils


import android.graphics.RectF
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Rect2d

fun Rect2d.toRectF() = RectF(
    this.x.toFloat(),
    this.y.toFloat(),
    (this.x + this.width).toFloat(),
    (this.y + this.height).toFloat()
)

fun Rect.toRectF() = RectF(
    this.x.toFloat(),
    this.y.toFloat(),
    (this.x + this.width).toFloat(),
    (this.y + this.height).toFloat()
)

fun RectF.toViewCoordinates(viewWidth: Int, viewHeight: Int) = RectF(
    this.left * viewWidth,
    this.top * viewHeight,
    this.right * viewWidth,
    this.bottom * viewHeight
)