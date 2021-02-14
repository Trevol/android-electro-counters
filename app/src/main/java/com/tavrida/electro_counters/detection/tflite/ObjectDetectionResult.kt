@file:UseSerializers(RectFSerializer::class)

package com.tavrida.electro_counters.detection.tflite

import android.graphics.RectF
import com.tavrida.utils.RectFSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class ObjectDetectionResult(val location: RectF, val classId: Int, val score: Float)