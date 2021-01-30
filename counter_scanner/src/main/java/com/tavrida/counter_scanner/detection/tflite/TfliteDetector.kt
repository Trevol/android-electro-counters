package com.tavrida.electro_counters.detection.tflite.new_detector

import android.graphics.Bitmap
import android.graphics.RectF
import com.tavrida.counter_scanner.detection.ObjectDetectionResult
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.TensorOperator
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.support.tensorbuffer.TensorBufferFloat
import java.nio.ByteBuffer


class TfliteDetector(
    private val interpreter: Interpreter,
    targetHeight: Int = 320,
    targetWidth: Int = 320
) {
    constructor(
        modelFile: ByteBuffer,
        targetHeight: Int = 320,
        targetWidth: Int = 320
    ) : this(interpreter(modelFile), targetHeight, targetWidth)

    private val tensorImage = TensorImage()
    private val imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(targetHeight, targetWidth, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
        .add(NormalizePlusMinus1Op())
        .build()

    private val locations = arrayOf(Array(NUM_DETECTIONS) { FloatArray(4) })
    private val classIds = arrayOf(FloatArray(NUM_DETECTIONS))
    private val scores = arrayOf(FloatArray(NUM_DETECTIONS))
    private val outputBuffer = mapOf(
        0 to locations,
        1 to classIds,
        2 to scores,
        3 to FloatArray(1)
    )

    fun detect(img: Bitmap, scoreThreshold: Float): List<ObjectDetectionResult> {
        tensorImage.load(img)
        val processedImg = imageProcessor.process(tensorImage)
        interpreter.runForMultipleInputsOutputs(
            arrayOf(processedImg.buffer),
            outputBuffer
        )
        val detections = (0 until NUM_DETECTIONS)
            .filter { indx -> scores[0][indx] >= scoreThreshold }
            .map { indx ->
                ObjectDetectionResult(
                    location = locations[0][indx].let {
                        val (y1, x1, y2, x2) = it
                        RectF(
                            x1 * img.width,
                            y1 * img.height,
                            x2 * img.width,
                            y2 * img.height
                        )
                    },
                    classId = classIds[0][indx].toInt() + 1,
                    score = scores[0][indx]
                )
            }
        return detections
    }

    private companion object {
        const val NUM_DETECTIONS = 10

        private fun interpreter(modelFile: ByteBuffer): Interpreter {
            val options = Interpreter.Options()
            // options.addDelegate(GpuDelegate())
            // options.addDelegate(NnApiDelegate())
            // options.setUseNNAPI(true)

            options.setNumThreads(4)
            options.setUseXNNPACK(true)
            return Interpreter(modelFile, options).apply { allocateTensors() }
        }
    }
}

private class NormalizePlusMinus1Op : TensorOperator {
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