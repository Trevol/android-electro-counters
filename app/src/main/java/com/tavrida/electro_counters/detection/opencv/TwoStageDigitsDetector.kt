package com.tavrida.ElectroCounters.detection

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy
import com.tavrida.utils.*
import com.tavrida.utils.camera.YuvToRgbConverter
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream
import java.security.InvalidParameterException

class TwoStageDigitsDetector(
    val screenDetector: DarknetDetector,
    val digitsDetector: DarknetDetector,
    val context: Context,
    storageDirectory: File?
) {
    val storage = storageDirectory?.let { CounterDetectionStorage(storageDirectory) }

    init {
        Log.d("TTTTT-TTT", "${this::class.qualifiedName}.init")
    }

    private lateinit var bitmapBuffer: Bitmap
    private val rgbaMatBuffer = Mat()
    private val rgbMatBuffer = Mat()
    private val rgbRotatedMatBuffer = Mat()

    private val converter = YuvToRgbConverter(context)


    @SuppressLint("UnsafeExperimentalUsageError")
    fun detect(yuvImage: ImageProxy): Collection<ObjectDetectionResult> {
        if (!::bitmapBuffer.isInitialized) {
            bitmapBuffer = Bitmap.createBitmap(
                yuvImage.width, yuvImage.height, Bitmap.Config.ARGB_8888
            )
        }

        Log.d("TTT-TTT-TTT", "${yuvImage.imageInfo.rotationDegrees}   ${yuvImage.width}:${yuvImage.height}")

        val sensorRotationDegrees = yuvImage.imageInfo.rotationDegrees

        yuvImage.use { converter.yuvToRgb(yuvImage.image!!, bitmapBuffer) }
        Utils.bitmapToMat(bitmapBuffer, rgbaMatBuffer)
        (rgbaMatBuffer.type() == CvType.CV_8UC4).assert()
        Imgproc.cvtColor(rgbaMatBuffer, rgbMatBuffer, Imgproc.COLOR_RGBA2RGB)

        val detectorInput = rgbMatBuffer.compensateSensorRotation(rgbRotatedMatBuffer, sensorRotationDegrees)

        // saveFrame(context, count, sensorRotationDegrees, bitmapBuffer, rgbMatBuffer, detectorInput)
        // count++

        return screenDetector.detect(detectorInput).detections
    }

    fun Mat.compensateSensorRotation(dst: Mat, sensorRotationDegrees: Int): Mat {
        val rotateCode = when (sensorRotationDegrees) {
            0 -> return this
            90 -> Core.ROTATE_90_CLOCKWISE
            180 -> Core.ROTATE_180
            else -> throw InvalidParameterException("Unexpected value $sensorRotationDegrees for sensorRotationDegrees")
        }
        Core.rotate(this, dst, rotateCode)
        return dst
    }

    fun process(image: ImageProxy) {
        val (rgbMat, bgrMat) = image.jpeg2RgbBgrMats()
        process(rgbMat, bgrMat)
    }

    fun process(bgrMat: Mat) {
        val rgbMat = bgrMat.bgr2rgb()
        process(rgbMat, bgrMat)
    }

    private fun process(rgbMat: Mat, bgrMat: Mat) {
        // detect screen
        val counterResult = screenDetector.detect(rgbMat)
        // extract screen image
        val screenDetection =
            counterResult.detections.firstOrNull { r -> r.classId == screenClassId }

        var screenRgbImg: Mat? = null
        var digitsResult: DarknetDetector.DetectionResult? = null
        if (screenDetection != null) {
            screenRgbImg = rgbMat.roi(screenDetection.box.toRect(), 30, 10)
            // TODO("Or double padding relative to box dimensions!!!")
            digitsResult = digitsDetector.detect(screenRgbImg)
        }

        val screenBgrImg = screenRgbImg?.rgb2bgr()
        val (resultVisualization, counterVisualization, digitsVisualization) = visualize(
            ImgDetections(bgrMat.copy(), counterResult.detections),
            digitsResult?.let { ImgDetections(screenBgrImg!!.copy(), digitsResult.detections) }
        )

        storage?.saveResults(
            bgrMat,
            resultVisualization,
            counterVisualization,
            screenBgrImg,
            digitsVisualization,
            counterResult,
            digitsResult
        )
    }

    private fun visualize(counter: ImgDetections, digits: ImgDetections?): Triple<Mat, Mat, Mat?> {
        counter.detections.forEach {
            Imgproc.rectangle(
                counter.img,
                it.box.toRect(),
                bgrClassColors[it.classId],
                4
            )
        }

        if (digits == null)
            return Triple(counter.img, counter.img, null)

        val digitsOnlyImg = Mat(digits.img.size(), digits.img.type(), Scalar(0))
        digits.detections.forEach { d ->
            Imgproc.rectangle(digits.img, d.box.toRect(), bgrGreen, 1)
            Imgproc.rectangle(digitsOnlyImg, d.box.toRect(), bgrGreen, 1)

            val labelOrd = Point(d.box.x + 3, d.box.y + d.box.height - 3)

            Imgproc.putText(
                digitsOnlyImg,
                d.classId.toString(),
                labelOrd,
                Imgproc.FONT_HERSHEY_SIMPLEX,
                .65,
                bgrGreen
            )
        }
        val digitsVisualization = hstack(digits.img, digitsOnlyImg)
        val resultVisualization = vstack(
            digitsVisualization.resize(width = counter.img.width()),
            Mat(10, counter.img.cols(), counter.img.type(), Scalar(0)),
            counter.img
        )
        return Triple(resultVisualization, counter.img, digitsVisualization)
    }

    fun galleryFiles() = storage?.galleryFiles() ?: listOf()

    private data class ImgDetections(
        val img: Mat,
        val detections: Collection<ObjectDetectionResult>
    )

    companion object {
        var count = 0

        fun saveFrame(
            context: Context,
            count: Int,
            sensorRotation: Int,
            bitmapBuffer: Bitmap,
            rgbMatBuffer: Mat,
            rgbRotatedMatBuffer: Mat
        ) {
            val framesDir = File(context.filesDir, "frames")
            // val framesDir = Asset.fileInDownloads("frames")
            framesDir.mkdirs()

            val paddedCount = count.toString().padStart(4, '0')

            FileOutputStream(File(framesDir, "${paddedCount}_${sensorRotation}_bitmap.png")).use {
                bitmapBuffer.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            Imgcodecs.imwrite(
                File(framesDir, "${paddedCount}_${sensorRotation}_opencv.png").absolutePath,
                rgbMatBuffer.rgb2bgr()
            )
            Imgcodecs.imwrite(
                File(framesDir, "${paddedCount}_${sensorRotation}_opencv_rotated.png").absolutePath,
                rgbRotatedMatBuffer.rgb2bgr()
            )
        }

        const val screenClassId = 1
        private val bgrRed = Scalar(0, 0, 255)
        private val bgrGreen = Scalar(0, 255, 0)
        private val bgrClassColors = arrayOf(bgrRed, bgrGreen)
    }

}


