package com.tavrida.electro_counters.detection.tflite.new_detector

import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.TensorOperator
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.support.tensorbuffer.TensorBufferFloat

class NormalizePlusMinus1Op : TensorOperator {
    override fun apply(input: TensorBuffer): TensorBuffer {
        // clip to [-1, 1] range => [0, 2] => subtract -1 => [-1, 1]
        val values = input.floatArray
        for (i in values.indices) {
            values[i] = f * values[i] - 1f
        }

        val inputShape = input.shape
        val output = if (input.isDynamic) {
            TensorBufferFloat.createDynamic(DataType.FLOAT32)
        } else {
            TensorBufferFloat.createFixedSize(inputShape, DataType.FLOAT32)
        }

        output.loadArray(values, inputShape)
        return output
    }

    private companion object {
        const val f = 2f / 255f
    }
}