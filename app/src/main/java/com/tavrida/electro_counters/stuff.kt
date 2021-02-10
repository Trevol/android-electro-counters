package com.tavrida.electro_counters

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.camera.core.ImageProxy
import androidx.core.app.ActivityCompat

/*var framesStorage: FramesStorage? = null
private fun prepareFramesStorage() {
    (framesStorage == null).assert()
    framesStorage = prepareFramesStorage(STORAGE_DIR, filesDir)
}*/


/*override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_camera)
    syncAnalysisUIState()

    imageView_preview.setOnClickListener {
        startStopListener()
    }
    buttonStartStop.setOnClickListener {
        startStopListener()
    }

    if (hasPermissions(this)) {
        bindCameraUseCases()
        prepareFramesStorage()
    } else {
        ActivityCompat.requestPermissions(
            this, permissions.toTypedArray(), permissionsRequestCode
        )
    }
}*/

/*fun startStopListener() {
    stopped = !stopped
    framesStorage!!.toggleSession(started)
    syncAnalysisUIState()
}*/



/*@SuppressLint("UnsafeExperimentalUsageError")
fun analyzeImage(image: ImageProxy) {
    val inputBitmap = image.use {
        getBitmapBuffer(image.width, image.height)
            .apply { yuvToRgbConverter.yuvToRgb(image.image!!, this) }
            .compensateSensorRotation(image.imageInfo.rotationDegrees)
    }

    imageView_preview.post {
        if (started) {
            framesStorage!!.addFrame(inputBitmap)
        }
        imageView_preview.setImageBitmap(inputBitmap)
    }

}*/

/*
override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == permissionsRequestCode && hasPermissions(this)) {
        bindCameraUseCases()
        prepareFramesStorage()
    } else {
        finish() // If we don't have the required permissions, we can't run
    }
}

const val STORAGE_DIR = "tavrida-electro-counters"*/
