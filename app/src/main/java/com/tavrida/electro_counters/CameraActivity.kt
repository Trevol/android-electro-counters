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
import com.tavrida.electro_counters.detection.tflite.new_detector.TfliteDetector
import com.tavrida.utils.camera.YuvToRgbConverter
import com.tavrida.utils.compensateSensorRotation
import com.tavrida.utils.copy
import kotlinx.android.synthetic.main.activity_camera.*
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
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

    private val yuvToRgbConverter by lazy { YuvToRgbConverter(this) }
    private var __bitmapBuffer: Bitmap? = null

    private fun getBitmapBuffer(width: Int, height: Int): Bitmap {
        if (__bitmapBuffer == null) {
            __bitmapBuffer = Bitmap.createBitmap(
                width, height, Bitmap.Config.ARGB_8888
            )
        }
        return __bitmapBuffer!!
    }

    val framesStorage by lazy { FramesStorage(File(filesDir, "frames")) }

    val detector by lazy { detector(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        syncAnalysisUIState()

        /*imageView_preview.setOnClickListener {
            startStopListener()
        }
        buttonStartStop.setOnClickListener {
            startStopListener()
        }*/

        framesStorage //trigger creation
        detector
    }

    fun startStopListener() {
        stopped = !stopped
        framesStorage.toggleSession(started)
        syncAnalysisUIState()
    }

    private fun syncAnalysisUIState() {
        /*val r = if (stopped) R.drawable.start_128 else R.drawable.stop_128
        buttonStartStop.setBackgroundResource(r)*/
    }

    private fun bindCameraUseCases() = imageView_preview.post {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            // Camera provider is now guaranteed to be available
            val cameraProvider = cameraProviderFuture.get()

            //4x3 resolutions: 640×480, 800×600, 960×720, 1024×768, 1280×960, 1400×1050, 1440×1080 , 1600×1200, 1856×1392, 1920×1440, and 2048×1536
            // val (w, h) = 1280 to 960
            // val (w, h) = 1024 to 768
            // val (w, h) = 960 to 720
            val (w, h) = 800 to 600
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
                { analyzeImage(it) })

            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                this as LifecycleOwner, cameraSelector, imageAnalysis
            )

            imageView_preview.afterMeasured { setupAutoFocus(imageView_preview, camera!!) }

        }, ContextCompat.getMainExecutor(this))
    }


    private object roi {
        val w: Int = 400
        val h: Int = 180

        val roiPaint = Paint().apply {
            color = Color.rgb(0, 255, 0)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        fun roiBitmap(src: Bitmap): Pair<Bitmap, Rect> {
            val r = rect(src)
            return Bitmap.createBitmap(src, r.left, r.top, r.width(), r.height()) to r
        }

        inline fun rect(src: Bitmap): Rect {
            val centerX = src.width / 2.0f
            val centerY = src.height / 2.0f

            val halfW = w / 2f
            val halfH = h / 2f
            return Rect(
                (centerX - halfW).toInt(),
                (centerY - halfH).toInt(),
                (centerX + halfW).toInt(),
                (centerY + halfH).toInt()
            )
        }

        fun draw(img: Bitmap): Bitmap {
            val r = rect(img)
            Canvas(img).drawRect(r, roiPaint)
            return img
        }
    }

    var prev = System.currentTimeMillis()

    @SuppressLint("UnsafeExperimentalUsageError")
    fun analyzeImage(image: ImageProxy) {
        val inputBitmap = image.use {
            getBitmapBuffer(image.width, image.height)
                .apply { yuvToRgbConverter.yuvToRgb(image.image!!, this) }
                .compensateSensorRotation(image.imageInfo.rotationDegrees)
        }
        val current = System.currentTimeMillis()
        (current - prev).log2()
        prev = current

        val (roiImage, roiRect) = roi.roiBitmap(inputBitmap)
        val detections = detector.detect(roiImage, .2f)

        imageView_preview.post {
            val imageWithRoi = roi.draw(inputBitmap.copy())
            vizUtils.drawDetections(imageWithRoi, detections, roiRect)

            if (started) {
                framesStorage.addFrame(imageWithRoi)
            }
            imageView_preview.setImageBitmap(imageWithRoi)
        }

    }

    private object vizUtils {
        const val screenId = 11
        private val screenPaint = Paint(Color.rgb(0, 0, 255), strokeWidth = 2f)
        private val digitBoxPaint = Paint(Color.rgb(253, 212, 81), strokeWidth = 2f)
        private val digitPaint =
            Paint(
                Color.rgb(253, 212, 81), style = Paint.Style.FILL_AND_STROKE,
                strokeWidth = 1f, textSize = 20f
            )

        private inline fun paint(classId: Int) =
            if (classId == screenId) screenPaint else digitBoxPaint

        private inline fun TfliteDetector.ObjectDetection.isDigit() = classId != screenId

        private fun Paint(
            color: Int,
            style: Paint.Style = Paint.Style.STROKE,
            strokeWidth: Float = 1f,
            textSize: Float = 15f
        ) = Paint().apply {
            this.color = color
            this.style = style
            this.strokeWidth = strokeWidth
            this.textSize = textSize
        }

        private fun remap(src: RectF, x: Int, y: Int) = RectF(
            src.left + x,
            src.top + y,
            src.right + x,
            src.bottom + y
        )

        fun drawDetections(
            srcBmp: Bitmap,
            detections: List<TfliteDetector.ObjectDetection>,
            roiRect: Rect
        ) {
            val canvas = Canvas(srcBmp)
            for (d in detections) {
                val remappedRect = remap(d.location, roiRect.left, roiRect.top)
                canvas.drawRect(remappedRect, paint(d.classId))
                if (d.isDigit()) {
                    canvas.drawText(
                        (d.classId - 1).toString(),
                        remappedRect.left + 2,
                        remappedRect.top - 2,
                        digitPaint
                    )
                }
            }
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
        private val TAG = CameraActivity::class.java.simpleName + "_TAG"

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

        const val MODEL_FILE = "screen_digits_320_128.tflite"

        private fun detector(context: Context) =
            mapAssetFile(context, MODEL_FILE)
                .let { TfliteDetector(it, 128, 320) }

        private fun mapAssetFile(context: Context, fileName: String): ByteBuffer {
            val assetFd = context.assets.openFd(fileName)
            val start = assetFd.startOffset
            val length = assetFd.declaredLength
            return FileInputStream(assetFd.fileDescriptor).channel
                .map(FileChannel.MapMode.READ_ONLY, start, length)
        }

        fun setupAutoFocus(viewFinder: View, camera: Camera) {
            return
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


        // const val TAG = "ExampleInstrumentedTest_TAG"
        fun log(msg: String) = Log.d(TAG, msg)
        fun log(msg: Any) = log(msg.toString())
        fun String.log2() = log(this)
        fun Any.log2() = log(this)

    }
}


