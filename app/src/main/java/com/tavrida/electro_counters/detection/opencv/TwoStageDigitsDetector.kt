package com.tavrida.ElectroCounters.detection

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.tavrida.utils.*
import com.tavrida.utils.camera.YuvToRgbConverter
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File

class TwoStageDigitsDetector(
    val screenDetector: DarknetDetector,
    val digitsDetector: DarknetDetector,
    val context: Context,
    storageDirectory: File?
) {
    val storage = storageDirectory?.let { CounterDetectionStorage(storageDirectory) }


    private val imageConverter = ImageConverter(context)

    fun detect(image: Bitmap, imageId: Int): TwoStageDetectionResult? {
        val detectorInput = imageConverter.convert(image)

        val screenDetection = screenDetector.detect(detectorInput).detections
            .filter { it.classId == screenClassId }
            // if multiple screen detected - choose closest to image center
            .minBy { it.box.center().L2squared(image.center()) }
            ?: return null

        val (screenImg, screenRoi) = detectorInput.roi(screenDetection.box.toRect(), .2, .2)
        val digitsDetections = digitsDetector.detect(screenImg).detections
        // return screenLocation: RectF, screenImage: Bitmap, digitsDetections (inside screen image)
        return TwoStageDetectionResult(
            screenRoi.toRectF(),
            screenImg.toBitmap(),
            digitsDetections
        )
    }

    fun detect(yuvImage: ImageProxy, imageId: Int): Collection<ObjectDetectionResult> {
        val detectorInput = imageConverter.convert(yuvImage)
        /*val screenDetection = screenDetector.detect(detectorInput).detections
            .filter { it.classId == screenClassId }
            .minBy { it.box.center().L2squared() }*/

        // return screenLocation: RectF, screenImage: Bitmap, digitsDetections (inside screen image)
        return listOf()
    }

    companion object {
        fun save(
            context: Context,
            imageId: Int,
            sensorRotation: Int,
            image: Mat
        ) {
            val framesDir = File(context.filesDir, "frames")
            // val framesDir = Asset.fileInDownloads("frames")
            framesDir.mkdirs()
            val paddedCount = imageId.toString().padStart(5, '0')
            Imgcodecs.imwrite(
                File(framesDir, "${paddedCount}_${sensorRotation}.png").absolutePath,
                image.rgb2bgr()
            )
        }

        const val screenClassId = 1
        private val bgrRed = Scalar(0, 0, 255)
        private val bgrGreen = Scalar(0, 255, 0)
        private val bgrClassColors = arrayOf(bgrRed, bgrGreen)
    }

    private class ImageConverter(context: Context) {
        private lateinit var bitmapBuffer: Bitmap
        private val rgbaMatBuffer = Mat()
        private val rgbMatBuffer = Mat()
        private val rgbRotatedMatBuffer = Mat()

        private val converter = YuvToRgbConverter(context)

        @SuppressLint("UnsafeExperimentalUsageError")
        fun convert(yuvImage: ImageProxy): Mat {
            if (!::bitmapBuffer.isInitialized) {
                bitmapBuffer = Bitmap.createBitmap(
                    yuvImage.width, yuvImage.height, Bitmap.Config.ARGB_8888
                )
            }
            converter.yuvToRgb(yuvImage.image!!, bitmapBuffer)
            Utils.bitmapToMat(bitmapBuffer, rgbaMatBuffer)
            Imgproc.cvtColor(rgbaMatBuffer, rgbMatBuffer, Imgproc.COLOR_RGBA2RGB)

            val detectorInput = rgbMatBuffer.compensateSensorRotation(
                rgbRotatedMatBuffer,
                yuvImage.imageInfo.rotationDegrees
            )
            return detectorInput
        }

        fun convert(image: Bitmap): Mat {
            Utils.bitmapToMat(image, rgbaMatBuffer)
            Imgproc.cvtColor(rgbaMatBuffer, rgbMatBuffer, Imgproc.COLOR_RGBA2RGB)
            return rgbMatBuffer
        }
    }


    /*fun process(image: ImageProxy) {
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
    )*/
}


