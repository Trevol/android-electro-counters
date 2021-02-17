@file:UseSerializers(RectFSerializer::class)
package com.tavrida.counter_scanner.aggregation

import android.graphics.RectF
import com.tavrida.utils.serialization.RectFSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class AggregatedDetections(val location: RectF, val score: Float, val digitsCounts: List<DigitCount>) {
    val totalCount = digitsCounts.sumOf { it.count }
    val digitWithMaxCount = digitsCounts.maxByOrNull { it.count }!!.digit
}

