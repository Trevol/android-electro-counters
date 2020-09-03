package com.tavrida.utils

import android.graphics.RectF
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Rect2d

fun Rect2d.toRect() = Rect(x.toInt(), y.toInt(), width.toInt(), height.toInt())
fun Rect2d.toDisplayStr() = "xywh( $x, $y, $width, $height )"

fun Rect2d.center() = Point(x + width / 2, y + height / 2)

fun Point.L2squared(p2: Point) =
    (x - p2.x).squared() + (y - p2.y).squared()

inline fun Double.squared() = this * this

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