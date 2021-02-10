package com.tavrida.counter_scanner.scanning

import android.graphics.Bitmap
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.tavrida.utils.copy
import java.lang.Exception
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class QRScanner(private val processNthImage: Int) {
    private enum class State { PROCESSING, IDLE }

    private object scanner {
        private val instance = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )

        fun process(image: Bitmap) = instance.process(InputImage.fromBitmap(image, 0))
    }

    private var state = State.IDLE
    private var imgCounter = 0
    private var barcodes = listOf<Barcode>()
    private var exception: Exception? = null

    private val lock = ReentrantLock()

    fun postProcess(image: Bitmap): List<Barcode> {
        return lock.withLock {
            if (state == State.IDLE) {
                val readyToProcess = imgCounter >= processNthImage
                imgCounter++
                if (readyToProcess) {
                    scanner.process(image)
                        .addOnSuccessListener { barcodes -> this.barcodes = barcodes }
                        .addOnFailureListener { }
                        .addOnCompleteListener { processingComplete(it) }
                    state = State.PROCESSING
                    imgCounter = 0
                }
            }
            barcodes
        }
    }

    fun barcodes() = lock.withLock { barcodes }

    private fun processingComplete(task: Task<MutableList<Barcode>>) {
        lock.withLock {
            if (task.exception != null) {
                exception = task.exception
            } else {
                barcodes = task.result
            }
            state = State.IDLE
        }
    }

}