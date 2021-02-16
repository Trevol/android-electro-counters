@file:UseSerializers(RectFSerializer::class)

package com.tavrida.counter_scanner.detection

import android.graphics.RectF
import com.tavrida.utils.serialization.RectFSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class ObjectDetectionResult(
    val classId: Int,
    val score: Float,
    val location: RectF
)