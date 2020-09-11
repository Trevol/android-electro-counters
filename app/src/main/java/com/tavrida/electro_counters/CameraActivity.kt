package com.tavrida.electro_counters

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.View
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.Camera
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
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/** Activity that displays the camera and performs object detection on the incoming frames */
class CameraActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()
    private val permissions = listOf(Manifest.permission.CAMERA)
    private val permissionsRequestCode = Random.nextInt(0, 10000)

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var stopped = true
    private val started get() = !stopped


    private val detectorProvider by lazy {
        TwoStageDigitsDetectorProvider(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        syncAnalysisUIState()

        imageView_preview.setOnClickListener {
            startStopListener()
        }
        buttonStartStop.setOnClickListener {
            startStopListener()
        }
        detectorProvider.ensureDetector()
    }

    fun startStopListener() {
        stopped = !stopped
        syncAnalysisUIState()
    }

    private fun syncAnalysisUIState() {
        val r = if (stopped) R.drawable.start_128 else R.drawable.stop_128
        buttonStartStop.setBackgroundResource(r)
        textView_timings.visibility = if (stopped) View.INVISIBLE else View.VISIBLE
    }

    private fun bindCameraUseCases() = imageView_preview.post {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            // Camera provider is now guaranteed to be available
            val cameraProvider = cameraProviderFuture.get()

            //4x3 resolutions: 640×480, 800×600, 960×720, 1024×768, 1280×960, 1400×1050, 1440×1080 , 1600×1200, 1856×1392, 1920×1440, and 2048×1536
            val (w, h) = 1280 to 960
            val targetRes = when (imageView_preview.display.rotation) {
                Surface.ROTATION_90, Surface.ROTATION_270 -> Size(w, h)
                Surface.ROTATION_0, Surface.ROTATION_180 -> Size(h, w)
                else -> throw Exception("Unexpected display.rotation ${imageView_preview.display.rotation}")
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(targetRes)
                .setTargetRotation(imageView_preview.display.rotation)
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

            imageView_preview.afterMeasured { setupAutoFocus(imageView_preview, camera!!) }

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

        if (started) {
            val result = detectorProvider.detector.detect(inputBitmap, imageId)
            val t1 = System.currentTimeMillis()
            showDetectionResults(imageId, inputBitmap, result, t1 - t0)
            imageId++
        } else {
            //simply show original frame
            imageView_preview.post {
                textView_timings.text = ""
                imageView_preview.setImageBitmap(inputBitmap)
                imageView_screen.visibility = View.GONE
                imageView_digits.visibility = View.GONE
            }
        }
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
        color = Color.argb(255, 255, 0, 0)
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 1f
        textSize = 24f
    }

    private val textPaintFontSizeSetter = PaintFontSizeSetter(textPaint)

    private class PaintFontSizeSetter(val paint: Paint) {
        private val refSize = 50f
        private val refText = "0"
        private val refBounds = Rect()

        init {
            precalcReferenceSize()
        }

        private fun precalcReferenceSize() {
            paint.textSize = refSize
            paint.getTextBounds(refText, 0, refText.length, refBounds)

        }

        fun setTextSizeForWidth(desiredWidth: Float, text: String) {
            // got here https://stackoverflow.com/a/21895626
            val desiredTextSize: Float = refSize * desiredWidth / refBounds.width()
            paint.textSize = desiredTextSize
        }

        fun setTextSizeForHeight(desiredHeight: Float, text: String) {
            // got here https://stackoverflow.com/a/21895626
            val desiredTextSize: Float = refSize * desiredHeight / refBounds.height()
            paint.textSize = desiredTextSize
        }
    }

    private fun showDetectionResults(
        imageId: Int,
        inputBitmap: Bitmap,
        detectionResult: TwoStageDetectionResult?,
        duration: Long
    ) {
        val timingTxt = "${duration}ms"
        // Log.d(TAG, "Process frame in $timingTxt")

        if (detectionResult == null) {
            imageView_preview.post {
                textView_timings.text = timingTxt + "  ${inputBitmap.width}x${inputBitmap.height}"
                imageView_preview.setImageBitmap(inputBitmap)
                imageView_screen.visibility = View.GONE
                imageView_digits.visibility = View.GONE
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

            val text = d.classId.toString()
            //calc and set fontSize to fit in box
            // TODO("setTextSizeForBox")
            textPaintFontSizeSetter.setTextSizeForHeight(boxF.height() - 4, text)
            digitsImageCanvas.drawText(
                text,
                boxF.left + 2,
                boxF.bottom - 2,
                textPaint
            )
        }

        // saveImages(imageId, inputBitmap, detectionResult.screenImage, digitsDetectionBitmap)

        imageView_preview.post {
            textView_timings.text = "$timingTxt  ${inputBitmap.width}x${inputBitmap.height}"
            imageView_preview.setImageBitmap(inputBitmap)

            imageView_screen.setImageBitmap(detectionResult.screenImage)
            imageView_digits.setImageBitmap(digitsDetectionBitmap)
            imageView_screen.visibility = View.VISIBLE
            imageView_digits.visibility = View.VISIBLE
        }
    }


    private fun saveImages(
        imageId: Int,
        inputImage: Bitmap,
        screenImage: Bitmap,
        digitsImage: Bitmap
    ) {
        val framesDir = File(filesDir, "results").apply { mkdirs() }
        val num = imageId.padStartEx(4, '0')
        inputImage.save(File(framesDir, "${num}_input.jpg"))
        screenImage.save(File(framesDir, "${num}_screen.jpg"))
        digitsImage.save(File(framesDir, "${num}_digits.jpg"))
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
            //see also https://developer.android.com/training/camerax/configuration#control-focus
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

        fun Bitmap.save(file: File) = FileOutputStream(file).use { fs ->
            this.compress(Bitmap.CompressFormat.JPEG, 90, fs)
        }

        fun Any.padStartEx(length: Int, padChar: Char) = toString().padStart(length, padChar)

    }
}