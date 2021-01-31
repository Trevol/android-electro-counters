package com.tavrida.counter_scanner.utils

import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Rect2d
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

inline fun Rect2d(x: Int, y: Int, width: Int, height: Int) =
    Rect2d(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

inline fun Rect2d(x: Float, y: Float, width: Float, height: Float) =
    Rect2d(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

fun Rect2d.toRect() = Rect(x.roundToInt(), y.roundToInt(), width.roundToInt(), height.roundToInt())
fun Rect2d.toDisplayStr() = "xywh( $x, $y, $width, $height )"

fun Rect2d.center() = Point(x + width / 2, y + height / 2)

fun Point.L2squared(p2: Point) =
    (x - p2.x).squared() + (y - p2.y).squared()

fun Rect.toRect2d() = Rect2d(x, y, width, height)

inline fun Double.squared() = this * this

fun Rect.overlap(other: Rect) = this.iou(other)

fun Rect.iou(other: Rect): Float {
    val intersectArea = this.intersection(other).area()
    return (intersectArea / (area() + other.area() - intersectArea)).toFloat()
}

fun Rect.intersection(other: Rect): Rect {
    val x1 = max(x, other.x)
    val y1 = max(y, other.y)
    val w = min(x + width, other.x + other.width) - x1
    val h = min(y + height, other.y + other.height) - y1
    if (w <= 0 || h <= 0)
        return Rect(0, 0, 0, 0)
    return Rect(x1, y1, w, h)
}


fun Rect2d.overlap(other: Rect2d) = this.iou(other)

fun Rect2d.iou(other: Rect2d): Float {
    val intersectArea = this.intersection(other).area()
    return (intersectArea / (area() + other.area() - intersectArea)).toFloat()
}

fun Rect2d.intersection(other: Rect2d): Rect2d {
    val x1 = max(x, other.x)
    val y1 = max(y, other.y)
    val w = min(x + width, other.x + other.width) - x1
    val h = min(y + height, other.y + other.height) - y1
    if (w <= 0 || h <= 0)
        return Rect2d(0, 0, 0, 0)
    return Rect2d(x1, y1, w, h)
}