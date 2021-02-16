package com.tavrida.electro_counters

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
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
import com.tavrida.counter_scanner.scanning.CounterScaningResult
import com.tavrida.counter_scanner.scanning.CounterScanner
import com.tavrida.counter_scanner.scanning.DetectionRoi
import com.tavrida.electro_counters.counter_scanner.CounterScannerProvider2
import com.tavrida.electro_counters.drawing.ScanResultDrawer
import com.tavrida.utils.assert
import kotlinx.android.synthetic.main.activity_camera.*
import org.opencv.android.OpenCVLoader
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class CameraActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()
    private val permissions =
        listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    private val permissionsRequestCode = Random.nextInt(0, 10000)

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK

    //4x3 resolutions: 640×480, 800×600, 960×720, 1024×768, 1280×960, 1400×1050, 1440×1080 , 1600×1200, 1856×1392, 1920×1440, and 2048×1536
    private val cameraRes = Size(640, 480)

    private var scanningStopped = true

    private inline val scanningStarted get() = !scanningStopped
    private val cameraImageConverter by lazy { CameraImageConverter2(this) }
    private val counterScannerProvider by lazy { CounterScannerProvider2() }

    private val detectorRoi = DetectionRoi(Size(400, 180))

    private object roiPaint {
        val started = Paint().apply {
            color = Color.rgb(0xFF, 0xC1, 0x07) //255,193,7 Color.rgb(0, 255, 0) //0xFFC107
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val stopped = Paint().apply {
            // color = Color.rgb(125, 63, 7) //255-130,193-130,7 Color.rgb(0, 255, 0) //0xFFC107
            color = Color.argb(55, 0xFF, 0xC1, 0x07)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
    }

    var counterScanner: CounterScanner? = null

    private val storage by lazy { AppStorage(this, STORAGE_DIR) }
    private var framesRecorder: FramesRecorder? = null
    private fun prepareFramesRecorder() {
        (framesRecorder == null).assert()
        framesRecorder = FramesRecorder(storage, enabled = loggingEnabled)
    }

    private fun initLazyVars() {
        //init lazies
        cameraImageConverter
        counterScannerProvider
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        syncAnalysisUIState()

        if (hasPermissions(this)) {
            bindCameraUseCases()
            prepareFramesRecorder()
        } else {
            ActivityCompat.requestPermissions(
                this, permissions.toTypedArray(), permissionsRequestCode
            )
        }

        imageView_preview.setOnClickListener {
            startStopListener()
        }
        recordingSwitch.isChecked = loggingEnabled
        recordingSwitch.setOnCheckedChangeListener { _, isChecked ->
            loggingEnabled = isChecked
            framesRecorder?.enabled = loggingEnabled
        }
        initLazyVars()
    }

    override fun onDestroy() {
        stopScanner()
        super.onDestroy()
    }

    override fun onPause() {
        scanningStopped = true
        stopScanner()
        super.onPause()
    }

    fun startStopListener() {
        val stopped = !scanningStopped
        if (stopped) {
            stopScanner()
        } else {
            counterScanner = counterScannerProvider.createScanner(this, detectorRoi)
        }
        // update flag at the end
        // because vars are accessed in separate thread (analyzeImage)
        this.scanningStopped = stopped
        framesRecorder!!.toggleSession(scanningStarted)
        syncAnalysisUIState()
    }

    private fun stopScanner() {
        counterScanner?.stop()
        counterScanner = null
    }

    private fun syncAnalysisUIState() {
        view_TapToStart.visibility = if (scanningStopped) View.VISIBLE else View.INVISIBLE
        val infoVisibility = if (scanningStopped) View.INVISIBLE else View.VISIBLE
        textView_timings.visibility = infoVisibility
        textView_timings.text = ""
        textView_readings.visibility = infoVisibility
        textView_readings.text = ""
        textView_clientId.visibility = infoVisibility
        textView_clientId.text = ""
    }

    private fun bindCameraUseCases() = imageView_preview.post {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            // Camera provider is now guaranteed to be available
            val cameraProvider = cameraProviderFuture.get()

            val targetRes = when (imageView_preview.display.rotation) {
                Surface.ROTATION_90, Surface.ROTATION_270 -> cameraRes
                Surface.ROTATION_0, Surface.ROTATION_180 -> Size(cameraRes.height, cameraRes.width)
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

            imageView_preview.afterMeasured { setupAutoFocus(imageView_preview, camera) }

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

    fun analyzeImage(image: ImageProxy) {
        val bitmap = image.use {
            if (scanningStopped) {
                Thread.sleep(150) //slow down fps in stopped mode
            }
            cameraImageConverter.convert(it)
        }

        if (scanningStarted) {
            framesRecorder!!.addFrame(bitmap)
            val result = counterScanner!!.scan(bitmap)
            detectorRoi.draw(bitmap, roiPaint.started)
            showDetectionResults(bitmap, result, measureAnalyzeImageCall())
        } else {
            //simply show original frame
            imageView_preview.post {
                detectorRoi.draw(bitmap, roiPaint.stopped)
                imageView_preview.setImageBitmap(bitmap)
            }
        }
    }

    private fun showDetectionResults(
        inputBitmap: Bitmap,
        scanResult: CounterScaningResult,
        duration: Long
    ) {
        if (scanningStopped) {
            return
        }
        imageView_preview.post {
            val readings = scanResult.readingInfo?.reading
            val imageWithDrawings = ScanResultDrawer().draw(inputBitmap, scanResult)
            textView_timings.text = "${duration}ms  ${inputBitmap.width}x${inputBitmap.height}"
            imageView_preview.setImageBitmap(imageWithDrawings)

            textView_readings.text = if (readings.isNullOrEmpty()) "" else readings
            textView_clientId.text = scanResult.consumerInfo?.consumerId
        }
    }


    override fun onResume() {
        super.onResume()
        bindCameraUseCases()
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
            prepareFramesRecorder()
        } else {
            finish() // If we don't have the required permissions, we can't run
        }
    }

    private fun hasPermissions(context: Context) = permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        init {
            OpenCVLoader.initDebug()
        }

        //global var - value should be preserved between activity recreations
        var loggingEnabled = false

        const val STORAGE_DIR = "tavrida-electro-counters"
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
    }
}


