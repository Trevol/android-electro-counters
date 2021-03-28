package com.tavrida.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.opencv.core.CvType
import org.opencv.core.Mat
import java.io.*
import java.nio.ByteBuffer

fun OutputStream.write(b: ByteBuffer) = write(b.array())

fun OutputStream.writeShort(value: Short) {
    ByteBuffer.allocate(Short.SIZE_BYTES)
        .apply { putShort(value) }
        .apply { write(this) }
}

fun InputStream.readShort() = ByteArray(Short.SIZE_BYTES)
    .apply { read(this) }
    .let { ByteBuffer.wrap(it).getShort(0) }

fun InputStream.read(b: ByteBuffer) = read(b.array())

fun Bitmap.pixelsBuffer() = ByteBuffer.allocateDirect(width * height * 4)
    .apply { copyPixelsToBuffer(this) }

fun Bitmap.toRawPixelData(stream: OutputStream) {
    stream.writeShort(width.toShort())
    stream.writeShort(height.toShort())
    stream.write(pixelsBuffer())
}

fun InputStream.rawPixelDataToMat(): Mat {
    val width = readShort()
    val height = readShort()
    val pixelData = ByteArray(width * height * 4)
        .apply { read(this) }
    return Mat(height.toInt(), width.toInt(), CvType.CV_8UC4)
        .apply { put(0, 0, pixelData) }
}

fun InputStream.decodeRawPixelBuffer(): Bitmap {
    val width = readShort().toInt()
    val height = readShort().toInt()
    val pixelData = ByteArray(width * height * 4)
        .apply { read(this) }
        .let { ByteBuffer.wrap(it) }
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        .apply { copyPixelsFromBuffer(pixelData) }

}

fun Bitmap.saveAsRawPixelData(file: File) = FileOutputStream(file).use { fs ->
    toRawPixelData(fs)
}

fun imreadAsRawPixelData(file: File) = FileInputStream(file).use {
    it.rawPixelDataToMat()
}

fun decodeRawPixelBuffer(file: File) = FileInputStream(file).use {
    it.decodeRawPixelBuffer()
}

object BitmapFactoryEx {
    fun decodeFile(file: File): Bitmap {
        if (file.extension == "pixel_data") {
            return decodeRawPixelBuffer(file)
        } else {
            return BitmapFactory.decodeFile(file.absolutePath)
        }
    }
}