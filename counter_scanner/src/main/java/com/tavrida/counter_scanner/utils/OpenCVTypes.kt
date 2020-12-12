package com.tavrida.counter_scanner.utils

fun Range(s: Double, e: Double) = org.opencv.core.Range(s.toInt(), e.toInt())
fun Size(width: Int, height: Int) = org.opencv.core.Size(width.toDouble(), height.toDouble())
fun Scalar(v0: Int, v1: Int, v2: Int) =
    org.opencv.core.Scalar(v0.toDouble(), v1.toDouble(), v2.toDouble())

fun Scalar(v0: Int) = org.opencv.core.Scalar(v0.toDouble())