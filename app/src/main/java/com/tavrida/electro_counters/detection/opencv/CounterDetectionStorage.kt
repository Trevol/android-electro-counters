package com.tavrida.ElectroCounters.detection

import com.tavrida.utils.assert
import com.tavrida.utils.androidInfo
import com.tavrida.utils.deviceName
import com.tavrida.utils.toDisplayStr
import org.opencv.core.Mat
import org.opencv.core.MatOfInt
import org.opencv.imgcodecs.Imgcodecs
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class CounterDetectionStorage(private val storageDirectory: File) {
    init {
        storageDirectory.mkdir()
    }

    companion object {
        private const val resultsJpg = "results.jpg"
        private const val TIMESTAMP_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private fun createTimestamp() = SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US).format(System.currentTimeMillis())
    }

    private val galleryFiles = listGalleryFiles().toMutableList()

    private fun listGalleryFiles(): List<File> {
        storageDirectory.isDirectory.assert()
        val files = storageDirectory
                .listFiles { f -> f.isDirectory }!!
                .map { dir ->
                    dir.listFiles { f -> f.name.endsWith(resultsJpg) }!!
                            .firstOrNull()
                }
                .filter { it != null }
                .map { it!! }
                .sortedByDescending { it!!.parentFile }
                .toMutableList()
        return files
    }

    fun saveResults(originalBgr: Mat, resultVisualization: Mat, counterVisualization: Mat,
                    screenImage: Mat?, digitsVisualization: Mat?,
                    counterResult: DarknetDetector.DetectionResult, digitsResult: DarknetDetector.DetectionResult?) {
        val (resultsDirectory, timestamp) = makeResultDirectory()

        saveImage(File(resultsDirectory, "${timestamp}.jpg"), originalBgr, 100)

        val resultsImgFile = File(resultsDirectory, "${timestamp}_${resultsJpg}")
        saveImage(resultsImgFile, resultVisualization)
        galleryFiles.add(0, resultsImgFile)

        saveImage(File(resultsDirectory, "${timestamp}_counter.jpg"), counterVisualization)
        if (screenImage != null)
            saveImage(File(resultsDirectory, "${timestamp}_screen.jpg"), screenImage)
        if (digitsVisualization != null)
            saveImage(File(resultsDirectory, "${timestamp}_digits.jpg"), digitsVisualization)

        saveInfo(
                File(resultsDirectory, "${timestamp}_info.txt"),
                listOf(Pair("CounterScreen", counterResult), Pair("Digits", digitsResult))
        )
    }

    private fun makeResultDirectory(): Pair<File, String> {
        val stamp = createTimestamp()
        return Pair(File(storageDirectory, stamp).apply { mkdir() }, stamp)
    }

    fun galleryFiles(): List<File> = galleryFiles.toList()

    private fun saveImage(file: File, bgrImg: Mat, jpegQuality: Int? = null) {
        val params = MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, jpegQuality
                ?: 50)
        Imgcodecs.imwrite(file.toString(), bgrImg, params)
    }

    private fun saveInfo(file: File, detectionResults: List<Pair<String, DarknetDetector.DetectionResult?>>) {
        val sb = StringBuilder()
        sb.appendln(deviceName()).appendln(androidInfo())

        detectionResults.forEachIndexed() { index, (name, detectionResult) ->
            sb.appendln("Detection stage #$index: $name")
            if (detectionResult == null) {
                sb.appendln("none")
            } else {
                sb.append("Duration (ms): ").appendln(detectionResult.durationInMs)
                detectionResult.detections.forEach {
                    sb.append("classId: ").append(it.classId)
                            .append("  classScore: ").append(it.classScore)
                            .append("  box: ").appendln(it.box.toDisplayStr())
                }
            }
            sb.appendln("-------------------------------------------")
        }

        FileOutputStream(file).use { fs ->
            fs.writer().use { wr ->
                wr.write(sb.toString())
            }
        }
    }


}