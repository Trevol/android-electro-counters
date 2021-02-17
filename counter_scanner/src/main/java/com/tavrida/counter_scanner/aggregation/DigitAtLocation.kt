@file:UseSerializers(RectFSerializer::class)

package com.tavrida.counter_scanner.aggregation

import android.graphics.RectF
import com.tavrida.utils.serialization.RectFSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class DigitAtLocation(val digit: Int, val location: RectF)