package com.tavrida.electro_counters

import android.graphics.Bitmap
import android.graphics.RectF
import com.tavrida.electro_counters.appstorage.AppStorage
import com.tavrida.electro_counters.detection.tflite.ObjectDetectionResult
import com.tavrida.utils.assert
import com.tavrida.utils.saveAsJpeg
import com.tavrida.utils.zeroPad
import kotlinx.serialization.KSerializer
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

class FramesStorage(storage: AppStorage, subDir: String = "frames") {
    private val storageDir = File(storage.root, subDir)
    private var framesPos = 0
    private var sessionId = ""
    private var started = false

    fun toggleSession(started: Boolean) =
        if (started) {
            startSession()
        } else {
            stopSession()
        }


    private fun startSession() {
        (!started).assert()
        storageDir.mkdirs()
        sessionId = createTimestamp()
        started = true
    }

    private fun stopSession() {
        started.assert()
        framesPos = 0
        sessionId = ""
        started = false
    }

    fun addFrame(frame: Bitmap, detections: List<ObjectDetectionResult>) {
        if (!started) {
            return
        }
        val paddedPos = framesPos.zeroPad(4)
        val baseName = "${sessionId}_${paddedPos}"
        frame.saveAsJpeg(File(storageDir, "$baseName.jpg"), quality = 75)
        detections.toJsonString().save(File(storageDir, "${baseName}_detections.json"))
        framesPos++
    }


    private companion object {
        private const val TIMESTAMP_FORMAT = "yyyyMMddHHmmss"
        private fun createTimestamp() =
            SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US).format(System.currentTimeMillis())

        private inline fun <reified T> T.toJsonString() = Json.encodeToString(this)

        private fun String.save(file: File) = FileOutputStream(file).use {
            OutputStreamWriter(it).use {
                it.write(this)
            }
        }
    }
}

private object RectFSerializer: KSerializer<RectF>{
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("", kind = PrimitiveKind.FLOAT)

    override fun deserialize(decoder: Decoder): RectF {
        // MapSerializer()
        TODO("Not yet implemented")
    }

    override fun serialize(encoder: Encoder, value: RectF) {
        TODO("Not yet implemented")
    }

}
// @file:UseSerializers
