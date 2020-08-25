package com.tavrida.electro_counters.detection.tflite

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import androidx.camera.core.ImageProxy
import com.tavrida.utils.camera.YuvToRgbConverter
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.image.ops.Rot90Op

class ObjectDetectionManager(context: Context) {

    init {
        init(context)
    }

    private var imageRotationDegrees: Int = 0
    private lateinit var bitmapBuffer: Bitmap
    private val tfImageBuffer = TensorImage(DataType.UINT8)

    private val converter = YuvToRgbConverter(context)
    private val tfInputSize by lazy {
        val inputIndex = 0
        val inputShape = tflite.getInputTensor(inputIndex).shape()
        Size(inputShape[2], inputShape[1]) // Order of axis is: {1, height, width, 3}
    }

    private val tfImageProcessor by lazy {
        val cropSize = minOf(bitmapBuffer.width, bitmapBuffer.height)
        ImageProcessor.Builder()
            .add(ResizeWithCropOrPadOp(cropSize, cropSize))
            .add(
                ResizeOp(
                    tfInputSize.height, tfInputSize.width, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR
                )
            )
            .add(Rot90Op(-imageRotationDegrees / 90))
            .add(NormalizeOp(0f, 1f))
            .build()
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    fun detect(yuvImage: ImageProxy): List<ObjectPrediction> {
        if (!::bitmapBuffer.isInitialized) {
            // The image rotation and RGB image buffer are initialized only once
            // the analyzer has started running
            imageRotationDegrees = yuvImage.imageInfo.rotationDegrees
            bitmapBuffer = Bitmap.createBitmap(
                yuvImage.width, yuvImage.height, Bitmap.Config.ARGB_8888
            )
        }

        yuvImage.use { converter.yuvToRgb(yuvImage.image!!, bitmapBuffer) }

        val tfImage = tfImageProcessor.process(tfImageBuffer.apply { load(bitmapBuffer) })
        val predictions = detector.predict(tfImage)
        return predictions.filter { it.score >= ACCURACY_THRESHOLD }
    }

    companion object {
        lateinit var detector: ObjectDetector
        lateinit var tflite: Interpreter

        fun init(context: Context) {
            if (!this::detector.isInitialized) {
                val opts = Interpreter.Options()
                    // .addDelegate(NnApiDelegate())
                    .addDelegate(GpuDelegate())
                tflite = Interpreter(FileUtil.loadMappedFile(context, MODEL_PATH), opts)
                detector = ObjectDetector(tflite, FileUtil.loadLabels(context, LABELS_PATH))
            }
        }

        private const val ACCURACY_THRESHOLD = 0.5f
        private const val MODEL_PATH = "coco_ssd_mobilenet_v1_1.0_quant.tflite"

        // private const val MODEL_PATH = "mobile_ssd_v2_float_coco.tflite"
        private const val LABELS_PATH = "coco_ssd_mobilenet_v1_1.0_labels.txt"
    }
}