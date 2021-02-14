package com.tavrida.utils

import android.graphics.RectF
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure

object RectFSerializer : KSerializer<RectF> {
    private const val LEFT = 0
    private const val TOP = 1
    private const val RIGHT = 2
    private const val BOTTOM = 3

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("RectF") {
        element<Float>("left")
        element<Float>("top")
        element<Float>("right")
        element<Float>("bottom")
    }

    override fun deserialize(decoder: Decoder): RectF {
        return decoder.decodeStructure(descriptor) {
            val r = RectF()

            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    LEFT -> r.left = decodeFloatElement(
                        descriptor,
                        index
                    )
                    TOP -> r.top = decodeFloatElement(
                        descriptor,
                        index
                    )
                    RIGHT -> r.right = decodeFloatElement(
                        descriptor,
                        index
                    )
                    BOTTOM -> r.bottom = decodeFloatElement(
                        descriptor,
                        index
                    )
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
            r
        }

    }

    override fun serialize(encoder: Encoder, value: RectF) {
        val structure = encoder.beginStructure(descriptor)
        structure.encodeFloatElement(descriptor, LEFT, value.left)
        structure.encodeFloatElement(descriptor, TOP, value.top)
        structure.encodeFloatElement(descriptor, RIGHT, value.right)
        structure.encodeFloatElement(descriptor, BOTTOM, value.bottom)
        structure.endStructure(descriptor)
    }

}