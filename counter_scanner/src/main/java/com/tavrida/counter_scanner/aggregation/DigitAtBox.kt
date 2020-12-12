package com.tavrida.counter_scanner.aggregation

import org.opencv.core.Rect2d

data class DigitAtBox(val digit: Int, val box: Rect2d)