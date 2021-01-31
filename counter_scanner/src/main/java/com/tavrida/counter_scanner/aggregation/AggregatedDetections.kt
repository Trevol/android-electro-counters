package com.tavrida.counter_scanner.aggregation

import android.graphics.RectF

data class AggregatedDetections(val location: RectF, val score: Float, val digitsCounts: List<DigitCount>) {
    val totalCount = digitsCounts.sumOf { it.count }
    val digitWithMaxCount = digitsCounts.maxByOrNull { it.count }!!.digit
}

