package com.tavrida.utils.serialization

import android.graphics.Rect
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure

object RectSerializer : KSerializer<Rect> {
    private const val LEFT = 0
    private const val TOP = 1
    private const val RIGHT = 2
    private const val BOTTOM = 3

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Rect") {
        element<Int>("left")
        element<Int>("top")
        element<Int>("right")
        element<Int>("bottom")
    }

    override fun deserialize(decoder: Decoder): Rect {
        return decoder.decodeStructure(descriptor) {
            val r = Rect()

            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    LEFT -> r.left = decodeIntElement(
                        descriptor,
                        index
                    )
                    TOP -> r.top = decodeIntElement(
                        descriptor,
                        index
                    )
                    RIGHT -> r.right = decodeIntElement(
                        descriptor,
                        index
                    )
                    BOTTOM -> r.bottom = decodeIntElement(
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

    override fun serialize(encoder: Encoder, value: Rect) {
        val structure = encoder.beginStructure(descriptor)
        structure.encodeIntElement(descriptor, LEFT, value.left)
        structure.encodeIntElement(descriptor, TOP, value.top)
        structure.encodeIntElement(descriptor, RIGHT, value.right)
        structure.encodeIntElement(descriptor, BOTTOM, value.bottom)
        structure.endStructure(descriptor)
    }

}