package com.tavrida.electro_counters

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
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
import com.tavrida.counter_scanner.scanning.nonblocking.NonblockingCounterReadingScanner
import com.tavrida.counter_scanner.utils.copy
import com.tavrida.electro_counters.counter_scanner.CounterScannerProvider
import com.tavrida.electro_counters.drawing.ScanResultDrawer
import com.tavrida.utils.*
import com.tavrida.utils.camera.YuvToRgbConverter
import kotlinx.android.synthetic.main.activity_camera.*
import org.opencv.android.OpenCVLoader
import java.io.File
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
    private var recordingEnabled = false

    private val yuvToRgbConverter by lazy { YuvToRgbConverter(this) }
    private val imageConverter = Bitmap2RgbMatConverter()
    private var __bitmapBuffer: Bitmap? = null

    private fun getBitmapBuffer(width: Int, height: Int): Bitmap {
        if (__bitmapBuffer == null) {
            __bitmapBuffer = Bitmap.createBitmap(
                width, height, Bitmap.Config.ARGB_8888
            )
        }
        return __bitmapBuffer!!
    }

    private val counterScannerProvider by lazy { CounterScannerProvider(this) }

    var counterScanner: NonblockingCounterReadingScanner? = null

    val detectionLogger by lazy {
        DetectionLogger(recordingEnabled, logDir = File(filesDir, "detections_log"))
    }

    private fun initLazyVars() {
        //init lazies
        yuvToRgbConverter
        counterScannerProvider
        detectionLogger
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
        recordingSwitch.isChecked = recordingEnabled
        recordingSwitch.setOnCheckedChangeListener { compoundButton, isChecked ->
            recordingEnabled = isChecked
            detectionLogger.loggingEnabled = recordingEnabled
        }
        initLazyVars()
    }

    override fun onDestroy() {
        stopScanner()
        super.onDestroy()
    }

    override fun onPause() {
        stopped = true
        stopScanner()
        super.onPause()
    }

    fun startStopListener() {
        stopped = !stopped
        if (stopped) {
            stopScanner()
        } else {
            counterScanner = counterScannerProvider.counterScanner()
        }
        syncAnalysisUIState()
    }

    private fun stopScanner() {
        counterScanner?.stop()
        counterScanner = null
    }

    private fun syncAnalysisUIState() {
        val r = if (stopped) R.drawable.start_128 else R.drawable.stop_128
        buttonStartStop.setBackgroundResource(r)
        textView_timings.visibility = if (stopped) View.INVISIBLE else View.VISIBLE
        textView_timings.text = ""
    }

    private fun bindCameraUseCases() = imageView_preview.post {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            // Camera provider is now guaranteed to be available
            val cameraProvider = cameraProviderFuture.get()

            //4x3 resolutions: 640×480, 800×600, 960×720, 1024×768, 1280×960, 1400×1050, 1440×1080 , 1600×1200, 1856×1392, 1920×1440, and 2048×1536
            val w = 1280
            val h = 960
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

    var prevAnalyzeImageCallMs: Long? = null
    inline fun measureAnalyzeImageCall(): Long {
        val currentAnalyzeImageCallMs = System.currentTimeMillis()
        val callDuration =
            currentAnalyzeImageCallMs - (prevAnalyzeImageCallMs ?: currentAnalyzeImageCallMs)
        prevAnalyzeImageCallMs = currentAnalyzeImageCallMs
        return callDuration
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    fun analyzeImage(image: ImageProxy) {
        val inputBitmap = image.use {
            getBitmapBuffer(image.width, image.height)
                .apply { yuvToRgbConverter.yuvToRgb(image.image!!, this) }
                .compensateSensorRotation(image.imageInfo.rotationDegrees)
        }

        if (started) {
            val detectorInput = imageConverter.convert(inputBitmap)
                .copy() // !!!make COPY - because detectorInput queued to detectorJob!!!
            val result = counterScanner!!.scan(detectorInput)

            showDetectionResults(inputBitmap, result, measureAnalyzeImageCall())
        } else {
            //simply show original frame
            imageView_preview.post {
                imageView_preview.setImageBitmap(inputBitmap)
            }
        }
    }

    private fun showDetectionResults(
        inputBitmap: Bitmap,
        scanResult: NonblockingCounterReadingScanner.ScanResult,
        duration: Long
    ) {
        val timingTxt = "${duration}ms"
        // Log.d(TAG, "Process frame in $timingTxt")

        // val screenImage = inputBitmap.roi(detectionResult.screenBox)
        // val (inputBitmapWithDrawing, screenImageWithDrawing, digitsDetectionBitmap) = detectionDrawer.drawDetectionResults(
        //     inputBitmap.copy(),
        //     screenImage,
        //     scanResult
        // )


        /*detectionLogger.log(
            scanResult,
            inputBitmap,
            inputBitmapWithDrawing,
            screenImageWithDrawing,
            digitsDetectionBitmap,
            duration
        )*/
        val readings = scanResult.readingInfo?.reading
        val imageWithDrawings = ScanResultDrawer().draw(inputBitmap.copy(), scanResult)
        imageView_preview.post {
            textView_timings.text = "$timingTxt  ${inputBitmap.width}x${inputBitmap.height}"
            imageView_preview.setImageBitmap(imageWithDrawings)

            if (readings != null && readings.isNotEmpty()) {
                textView_readings.visibility = View.VISIBLE
                textView_readings.text = readings
                /*println("readings: $readings\n")
                println(scanResult.digitsAtBoxes)
                println("--------------------------------")*/
            } else {
                textView_readings.visibility = View.INVISIBLE
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
        syncAnalysisUIState()
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
        init {
            OpenCVLoader.initDebug()
        }

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


        fun Any.padStartEx(length: Int, padChar: Char) = toString().padStart(length, padChar)

    }
}


