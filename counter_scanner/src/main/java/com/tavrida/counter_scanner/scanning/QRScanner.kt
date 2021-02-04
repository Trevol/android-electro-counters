package com.tavrida.counter_scanner.scanning

import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

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

    private val state = AtomicReference(State.IDLE)
    private val imgCounter = AtomicInteger(0)
    private val barcodes = AtomicReference(listOf<Barcode>())

    //TODO: может просто блокировать критической секцией? Вместо набора atomic-значений?

    fun postProcess(image: Bitmap): List<Barcode>? {
        if (state.get() == State.PROCESSING) {
            return barcodes.get()
        }
        //IDLE state
        val readyToProcess = imgCounter.getAndIncrement() % processNthImage == 0
        if (readyToProcess) {
            scanner.process(image)
                .addOnSuccessListener { barcodes -> this.barcodes.set(barcodes) }
                .addOnFailureListener { }
                .addOnCompleteListener { state.set(State.IDLE) }
            state.set(State.PROCESSING)
        }
        return barcodes.get()
    }

}