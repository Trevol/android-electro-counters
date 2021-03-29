package com.tavrida.electro_counters

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Size
import android.view.KeyEvent
import android.view.Surface
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.tavrida.utils.assert
import com.tavrida.utils.iif
import com.tavrida.utils.toggle
import kotlinx.android.synthetic.main.activity_camera.*
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
    private var camera: Camera? = null
    private val flashlightControl = FlashlightControl({ flashSwitch }) { camera!! }

    //4x3 resolutions: 640×480, 800×600, 960×720, 1024×768, 1280×960, 1400×1050, 1440×1080 , 1600×1200, 1856×1392, 1920×1440, and 2048×1536
    private val cameraRes = Size(640, 480)

    private var initialized = false

    private val controller by lazy { CameraActivityController(this) }

    private inline fun initController() {
        controller
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        if (hasPermissions(this)) {
            initActivity()
        } else {
            ActivityCompat.requestPermissions(
                this, permissions.toTypedArray(), permissionsRequestCode
            )
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode in SCANNING_CONTROL_KEYS) {
            if (initialized) {
                toggleScanning()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun initActivity() {
        if (initialized) {
            return
        }

        initController()
        updateUI()
        imageView_preview.setOnClickListener {
            toggleScanning()
        }
        bindCameraUseCases()
        // TorchState
        // CameraManager.TorchCallback
        flashlightControl.syncUI()
        flashSwitch.setOnClickListener {
            flashlightControl.toggle()
        }
        initialized = true
    }

    override fun onDestroy() {
        super.onDestroy()
        if (initialized) {
            controller.stopScanner()
        }
    }

    override fun onPause() {
        super.onPause()
        if (initialized) {
            controller.stopScanner()
        }
    }

    private fun toggleScanning() {
        initialized.assert()
        controller.toggleScanning()
        updateUI()
    }


    private fun updateUI() {
        if (!initialized) {
            return
        }
        view_TapToStart.visibility =
            if (controller.scanningStopped) View.VISIBLE else View.INVISIBLE
        val infoVisibility = if (controller.scanningStopped) View.INVISIBLE else View.VISIBLE
        textView_timings.visibility = infoVisibility
        textView_timings.text = ""
        textView_readings.visibility = infoVisibility
        textView_readings.text = ""
        textView_clientId.visibility = infoVisibility
        textView_clientId.text = ""
    }

    private fun bindCameraUseCases() = imageView_preview.post {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
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
            camera = cameraProvider.bindToLifecycle(
                this as LifecycleOwner, cameraSelector, imageAnalysis
            )
            imageView_preview.afterMeasured { setupAutoFocus(imageView_preview, camera!!) }

        }, ContextCompat.getMainExecutor(this))
    }

    private inline fun slowdownIfStopped() {
        if (controller.scanningStopped) {
            Thread.sleep(100)
        }
    }

    private fun analyzeImage(image: ImageProxy) {
        slowdownIfStopped()
        val result = controller.analyzeImage(image)
        showResult(result)
    }

    private fun showResult(result: CameraActivityController.AnalyzeImageResult) {
        runOnUiThread {
            imageView_preview.setImageBitmap(result.displayImage)
            if (result.scanResultAndDuration != null) {
                val (scanResult, duration) = result.scanResultAndDuration
                val readings = scanResult.readingInfo?.reading
                textView_timings.text = "${duration}ms"

                textView_readings.text = readings
                textView_clientId.text = scanResult.consumerInfo?.consumerId
            } else {
                textView_readings.text = ""
                textView_clientId.text = ""
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (initialized) {
            updateUI()
            bindCameraUseCases()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionsRequestCode && hasPermissions(this)) {
            initActivity()
        } else {
            finish() // If we don't have the required permissions, we can't run
        }
    }

    private fun hasPermissions(context: Context) = permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        class FlashlightControl(val flashSwitch: () -> ImageView, val camera: () -> Camera) {
            private var enabled = false
            fun enabled() = enabled

            fun toggle() {
                enabled = enabled.toggle()
                camera().cameraControl.enableTorch(enabled)
                syncUI()
            }

            fun syncUI() {
                flashSwitch().setImageResource(
                    enabled.iif(R.drawable.icons8_torch_32_on, R.drawable.icons8_torch_32_off)
                )
            }
        }

        val SCANNING_CONTROL_KEYS = listOf(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN)

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


