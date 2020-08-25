/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tavrida.electro_counters

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.util.Size
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.tavrida.utils.camera.YuvToRgbConverter
import kotlinx.android.synthetic.main.activity_camera.*
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
import java.util.concurrent.Executors
import kotlin.random.Random

/** Activity that displays the camera and performs object detection on the incoming frames */
class CameraActivity : AppCompatActivity() {

    private lateinit var bitmapBuffer: Bitmap

    private val executor = Executors.newSingleThreadExecutor()
    private val permissions = listOf(Manifest.permission.CAMERA)
    private val permissionsRequestCode = Random.nextInt(0, 10000)

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK

    private var imageRotationDegrees: Int = 0
    private val tfImageBuffer = TensorImage(DataType.UINT8)

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

    private val tflite by lazy {
        Interpreter(
            FileUtil.loadMappedFile(this, MODEL_PATH),
//            Interpreter.Options().addDelegate(NnApiDelegate())
            Interpreter.Options().addDelegate(GpuDelegate())
        )
    }

    private val detector by lazy {
        ObjectDetectionHelper(
            tflite,
            FileUtil.loadLabels(this, LABELS_PATH)
        )
    }

    private val tfInputSize by lazy {
        val inputIndex = 0
        val inputShape = tflite.getInputTensor(inputIndex).shape()
        Size(inputShape[2], inputShape[1]) // Order of axis is: {1, height, width, 3}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
    }

    private fun bindCameraUseCases() = view_finder.post {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            // Camera provider is now guaranteed to be available
            val cameraProvider = cameraProviderFuture.get()

            // Set up the view finder use case to display camera preview
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(view_finder.display.rotation)
                .build()

            // Set up the image analysis use case which will process frames in real time
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(view_finder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val converter = YuvToRgbConverter(this)

            imageAnalysis.setAnalyzer(
                executor,
                ImageAnalysis.Analyzer { image -> analyzeImage(image, converter) })

            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                this as LifecycleOwner, cameraSelector, preview, imageAnalysis
            )

            preview.setSurfaceProvider(view_finder.createSurfaceProvider())

        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    fun analyzeImage(image: ImageProxy, converter: YuvToRgbConverter) {
        if (!::bitmapBuffer.isInitialized) {
            // The image rotation and RGB image buffer are initialized only once
            // the analyzer has started running
            imageRotationDegrees = image.imageInfo.rotationDegrees
            bitmapBuffer = Bitmap.createBitmap(
                image.width, image.height, Bitmap.Config.ARGB_8888
            )
        }

        val t0 = System.currentTimeMillis()

        image.use { converter.yuvToRgb(image.image!!, bitmapBuffer) }
        val tfImage = tfImageProcessor.process(tfImageBuffer.apply { load(bitmapBuffer) })
        val predictions = detector.predict(tfImage)

        val t1 = System.currentTimeMillis()

        val timingTxt = "${t1 - t0}ms"
        Log.d(TAG, "Process frame in $timingTxt")
        text_timings.post { text_timings.text = timingTxt }

        // Report only the top prediction
        reportPrediction(predictions.maxBy { it.score })
    }

    private fun reportPrediction(
        prediction: ObjectDetectionHelper.ObjectPrediction?
    ) = view_predictions.post {

        if (prediction == null || prediction.score < ACCURACY_THRESHOLD) {
            return@post
        }

        // Location has to be mapped to our local coordinates
        val location = mapOutputCoordinates(prediction.location)
        view_predictions.showLocations(listOf(location))
        // val predictionText = "${"%.2f".format(prediction.score)} ${prediction.label}"
        // text_prediction.text = predictionText
        // (box_prediction.layoutParams as ViewGroup.MarginLayoutParams).apply {
        //     topMargin = location.top.toInt()
        //     leftMargin = location.left.toInt()
        //     width = min(view_finder.width, location.right.toInt() - location.left.toInt())
        //     height = min(view_finder.height, location.bottom.toInt() - location.top.toInt())
        // }

        // box_prediction.visibility = View.VISIBLE
        // text_prediction.visibility = View.VISIBLE
    }

    /**
     * Helper function used to map the coordinates for objects coming out of
     * the model into the coordinates that the user sees on the screen.
     */
    private fun mapOutputCoordinates(location: RectF): RectF {

        // Step 1: map location to the preview coordinates
        val previewLocation = RectF(
            location.left * view_finder.width,
            location.top * view_finder.height,
            location.right * view_finder.width,
            location.bottom * view_finder.height
        )

        // Step 2: compensate for 1:1 to 4:3 aspect ratio conversion + small margin
        val margin = 0.1f
        val requestedRatio = 4f / 3f
        val midX = (previewLocation.left + previewLocation.right) / 2f
        val midY = (previewLocation.top + previewLocation.bottom) / 2f
        return if (view_finder.width < view_finder.height) {
            RectF(
                midX - (1f + margin) * requestedRatio * previewLocation.width() / 2f,
                midY - (1f - margin) * previewLocation.height() / 2f,
                midX + (1f + margin) * requestedRatio * previewLocation.width() / 2f,
                midY + (1f - margin) * previewLocation.height() / 2f
            )
        } else {
            RectF(
                midX - (1f - margin) * previewLocation.width() / 2f,
                midY - (1f + margin) * requestedRatio * previewLocation.height() / 2f,
                midX + (1f - margin) * previewLocation.width() / 2f,
                midY + (1f + margin) * requestedRatio * previewLocation.height() / 2f
            )
        }
    }

    override fun onResume() {
        super.onResume()

        // Request permissions each time the app resumes, since they can be revoked at any time
        if (hasPermissions(this)) {
            bindCameraUseCases()
        } else {
            ActivityCompat.requestPermissions(
                this, permissions.toTypedArray(), permissionsRequestCode
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionsRequestCode && hasPermissions(this)) {
            bindCameraUseCases()
        } else {
            finish() // If we don't have the required permissions, we can't run
        }
    }

    /** Convenience method used to check if all permissions required by this app are granted */
    private fun hasPermissions(context: Context) = permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private val TAG = CameraActivity::class.java.simpleName

        private const val ACCURACY_THRESHOLD = 0.5f
        private const val MODEL_PATH = "coco_ssd_mobilenet_v1_1.0_quant.tflite"
        private const val LABELS_PATH = "coco_ssd_mobilenet_v1_1.0_labels.txt"
    }
}