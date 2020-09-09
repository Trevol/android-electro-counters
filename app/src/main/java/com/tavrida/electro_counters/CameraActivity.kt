package com.tavrida.electro_counters

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.View
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.tavrida.ElectroCounters.detection.TwoStageDetectionResult
import com.tavrida.ElectroCounters.detection.TwoStageDigitsDetectorProvider
import com.tavrida.utils.camera.YuvToRgbConverter
import com.tavrida.utils.compensateSensorRotation
import com.tavrida.utils.toRectF
import kotlinx.android.synthetic.main.activity_camera.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/** Activity that displays the camera and performs object detection on the incoming frames */
class CameraActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()
    private val permissions = listOf(Manifest.permission.CAMERA)
    private val permissionsRequestCode = Random.nextInt(0, 10000)

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK

    private val detectorProvider by lazy {
        TwoStageDigitsDetectorProvider(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        detectorProvider.ensureDetector()
    }

    private fun bindCameraUseCases() = view_preview.post {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            // Camera provider is now guaranteed to be available
            val cameraProvider = cameraProviderFuture.get()

            //4x3 resolutions: 640×480, 800×600, 960×720, 1024×768, 1280×960, 1400×1050, 1440×1080 , 1600×1200, 1856×1392, 1920×1440, and 2048×1536
            val (w, h) = 1280 to 960
            val targetRes = when (view_preview.display.rotation) {
                Surface.ROTATION_90, Surface.ROTATION_270 -> Size(w, h)
                Surface.ROTATION_0, Surface.ROTATION_180 -> Size(h, w)
                else -> throw Exception("Unexpected display.rotation ${view_preview.display.rotation}")
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(targetRes)
                .setTargetRotation(view_preview.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(
                executor,
                ImageAnalysis.Analyzer { analyzeImage(it) })

            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                this as LifecycleOwner, cameraSelector, imageAnalysis
            )

            view_preview.afterMeasured { setupAutoFocus(view_preview, camera!!) }

        }, ContextCompat.getMainExecutor(this))
    }

    private val converter by lazy { YuvToRgbConverter(this) }
    private lateinit var bitmapBuffer: Bitmap

    @SuppressLint("UnsafeExperimentalUsageError")
    fun analyzeImage(image: ImageProxy) {
        val t0 = System.currentTimeMillis()

        val rotation = image.imageInfo.rotationDegrees
        if (!::bitmapBuffer.isInitialized) {
            bitmapBuffer = Bitmap.createBitmap(
                image.width, image.height, Bitmap.Config.ARGB_8888
            )
        }
        image.use {
            converter.yuvToRgb(image.image!!, bitmapBuffer)
        }
        val inputBitmap = bitmapBuffer.compensateSensorRotation(rotation)
        val result = detectorProvider.detector.detect(inputBitmap, imageId)

        val t1 = System.currentTimeMillis()

        showDetectionResults(inputBitmap, result, t1 - t0)

        imageId++

    }

    val screenPaint = Paint().apply {
        color = Color.argb(255, 255, 0, 0)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    val digitsPaint = Paint().apply {
        color = Color.argb(255, 0, 255, 0)
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    val textPaint = Paint().apply {
        color = Color.argb(255, 0, 255, 0)
        style = Paint.Style.STROKE
        strokeWidth = 1f
        textSize = 12f
    }

    private fun showDetectionResults(
        inputBitmap: Bitmap,
        detectionResult: TwoStageDetectionResult?,
        duration: Long
    ) {
        val timingTxt = "${duration}ms"
        // Log.d(TAG, "Process frame in $timingTxt")

        if (detectionResult == null) {
            view_preview.post {
                text_timings.text = timingTxt + "  ${inputBitmap.width}x${inputBitmap.height}"
                view_preview.setImageBitmap(inputBitmap)
                image_screen.visibility = View.GONE
                image_digits.visibility = View.GONE
            }
            return
        }
        val canvas = Canvas(inputBitmap)
        canvas.drawRect(detectionResult.screenLocation, screenPaint)

        val digitsDetectionBitmap = Bitmap.createBitmap(
            detectionResult.screenImage.width,
            detectionResult.screenImage.height,
            Bitmap.Config.ARGB_8888
        ).apply { eraseColor(Color.argb(255, 0, 0, 0)) }

        val screenImageCanvas = Canvas(detectionResult.screenImage)
        val digitsImageCanvas = Canvas(digitsDetectionBitmap)
        for (d in detectionResult.digitsDetections) {
            val boxF = d.box.toRectF()
            screenImageCanvas.drawRect(boxF, digitsPaint)
            digitsImageCanvas.drawRect(boxF, digitsPaint)
            // TODO("digitsImageCanvas.drawText()")
            digitsImageCanvas.drawText(
                d.classId.toString(),
                boxF.left + 2,
                boxF.bottom - 2,
                textPaint
            )
        }


        view_preview.post {
            text_timings.text = timingTxt + "  ${inputBitmap.width}x${inputBitmap.height}"
            view_preview.setImageBitmap(inputBitmap)

            image_screen.setImageBitmap(detectionResult.screenImage)
            image_digits.setImageBitmap(digitsDetectionBitmap)
            image_screen.visibility = View.VISIBLE
            image_digits.visibility = View.VISIBLE
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
        private var imageId = 0
        private val TAG = CameraActivity::class.java.simpleName

        inline fun View.afterMeasured(crossinline block: () -> Unit) {
            viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (measuredWidth > 0 && measuredHeight > 0) {
                        viewTreeObserver.removeOnGlobalLayoutListener(this)
                        block()
                    }
                }
            })
        }

        fun setupAutoFocus(viewFinder: View, camera: Camera) {
            val width = viewFinder.width.toFloat()
            val height = viewFinder.height.toFloat()

            val factory = SurfaceOrientedMeteringPointFactory(width, height)
            val cx = width / 2
            val cy = height / 2
            val afPoint = factory.createPoint(cx, cy)

            val focusMeteringAction =
                FocusMeteringAction.Builder(afPoint, FocusMeteringAction.FLAG_AF)
                    .setAutoCancelDuration(1, TimeUnit.SECONDS)
                    .build()
            camera.cameraControl.startFocusAndMetering(focusMeteringAction)
        }
    }
}