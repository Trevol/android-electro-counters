package com.tavrida.utils


import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import com.tavrida.counter_scanner.utils.Rect2d
import org.opencv.core.Rect2d
import kotlin.math.max
import kotlin.math.min

val RectF.x inline get() = left
val RectF.y inline get() = top

fun RectF(left: Int, top: Int, right: Int, bottom: Int) =
    RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())

fun RectF.toRect2d() = Rect2d(x, y, width(), height())

fun Rect2d.toRectF() = RectF(
    this.x.toFloat(),
    this.y.toFloat(),
    (this.x + this.width).toFloat(),
    (this.y + this.height).toFloat()
)

inline fun Rect.tl() = Point(left, top)
inline fun Rect.br() = Point(right, bottom)

fun RectF.toViewCoordinates(viewWidth: Int, viewHeight: Int) = RectF(
    this.left * viewWidth,
    this.top * viewHeight,
    this.right * viewWidth,
    this.bottom * viewHeight
)

fun RectF.area(): Float {
    return width() * height()
}

fun RectF.overlap(other: RectF) = this.iou(other)

fun RectF.iou(other: RectF): Float {
    val intersectArea = this.intersection(other).area()
    return (intersectArea / (area() + other.area() - intersectArea)).toFloat()
}

fun RectF.intersection(other: RectF): RectF {
    val x1 = max(left, other.left)
    val y1 = max(top, other.top)
    val w = min(right, other.right) - x1
    val h = min(bottom, other.bottom) - y1
    if (w <= 0 || h <= 0)
        return RectF(0f, 0f, 0f, 0f)
    return RectF(x1, y1, x1 + w, y1 + h)
}