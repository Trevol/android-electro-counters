package com.tavrida.electro_counters.detection.tflite.new_detector

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

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

    fun detect(img: Bitmap, scoreThreshold: Float): List<ObjectDetection> {
        tensorImage.load(img)
        val processedImg = imageProcessor.process(tensorImage)
        interpreter.runForMultipleInputsOutputs(
            arrayOf(processedImg.buffer),
            outputBuffer
        )
        val detections = (0 until NUM_DETECTIONS)
            .filter { indx -> scores[0][indx] >= scoreThreshold }
            .map { indx ->
                ObjectDetection(
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

    data class ObjectDetection(val location: RectF, val classId: Int, val score: Float)

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