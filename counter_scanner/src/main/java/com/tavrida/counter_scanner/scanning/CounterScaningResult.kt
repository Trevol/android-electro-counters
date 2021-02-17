@file:UseSerializers(RectSerializer::class)

package com.tavrida.counter_scanner.scanning

import android.graphics.Rect
import com.tavrida.counter_scanner.aggregation.AggregatedDetections
import com.tavrida.counter_scanner.aggregation.DigitAtLocation
import com.tavrida.utils.serialization.RectSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class CounterScaningResult(
    val digitsAtLocations: List<DigitAtLocation>,
    val aggregatedDetections: List<AggregatedDetections>,
    val readingInfo: ReadingInfo?,
    val consumerInfo: ConsumerInfo?
) {
    @Serializable
    data class ReadingInfo(val reading: String, val msOfStability: Long)

    @Serializable
    data class ConsumerInfo(
        val consumerId: String,
        val barcodeLocation: Rect?
    )

    companion object {
        fun empty() = CounterScaningResult(listOf(), listOf(), null, null)
    }
}