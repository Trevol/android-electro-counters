package com.tavrida.ElectroCounters.detection

import android.util.Log
import com.tavrida.utils.latterbox
import org.opencv.core.*
import org.opencv.dnn.Dnn
import org.opencv.dnn.Net
import java.util.*

class DarknetDetector {
    var confThreshold: Float = 0.3f
    var nmsThreshold: Float = 0.4f
    private var net: Net
    var outputLayers: List<String>
    var inputSize: Size

    constructor(
        net: Net,
        inputSize: Int = 416,
        confThreshold: Float = 0.3f,
        nmsThreshold: Float = 0.4f
    ) {
        this.net = net
        this.outputLayers = net.outputLayers()
        this.inputSize = Size(inputSize, inputSize)
        this.confThreshold = confThreshold
        this.nmsThreshold = nmsThreshold
    }

    constructor(
        cfgFile: String, darknetModel: String, inputSize: Int = 416, confThreshold: Float = 0.3f,
        nmsThreshold: Float = 0.4f
    ) : this(makeNet(cfgFile, darknetModel), inputSize, confThreshold, nmsThreshold)

    constructor(
        cfgFile: ByteArray,
        darknetModel: ByteArray,
        inputSize: Int = 416,
        confThreshold: Float = 0.3f,
        nmsThreshold: Float = 0.4f
    ) : this(makeNet(cfgFile, darknetModel), inputSize, confThreshold, nmsThreshold)

    init {
        Log.d("TTTTT-TTT", "${this::class.qualifiedName}.init")
    }

    data class DetectionResult(
        val detections: Collection<ObjectDetectionResult>,
        val durationInMs: Long
    )

    fun detect(rgbImg: Mat): DetectionResult {
        val t0 = System.currentTimeMillis()

        val inputBlob = preprocess(rgbImg, inputSize)
        net.setInput(inputBlob)

        val outputBlobs = ArrayList<Mat>()
        net.forward(outputBlobs, outputLayers)

        val detections = postprocess(rgbImg, outputBlobs, confThreshold, nmsThreshold)

        val t1 = System.currentTimeMillis()
        return DetectionResult(detections, t1 - t0)
    }

    fun preprocess(rgbImage: Mat, inputSize: Size): Mat {
        val (img) = latterbox(rgbImage, inputSize)
        val blob = Dnn.blobFromImage(img, 1 / 255.0)
        return blob
    }

    fun postprocess(
        frame: Mat,
        outs: List<Mat>,
        confThreshold: Float,
        nmsThreshold: Float
    ): Collection<ObjectDetectionResult> {

        val classIds = ArrayList<Int>()
        val classScores = ArrayList<Float>()
        val boxes = ArrayList<Rect2d>()
        for (detections in outs) {
            for (objectIdx in 0 until detections.rows()) {
                val objectConfidence = detections[objectIdx, 4][0]
                if (objectConfidence < confThreshold) {
                    continue
                }
                val scores = detections.row(objectIdx).colRange(5, detections.cols())
                val minMaxResult = Core.minMaxLoc(scores)
                val classId = minMaxResult.maxLoc.x.toInt()
                val classScore = minMaxResult.maxVal
                if (classScore < confThreshold) {
                    continue
                }
                val normalizedCenterX = detections[objectIdx, 0][0]
                val normalizedCenterY = detections[objectIdx, 1][0]
                val normalizedWidth = detections[objectIdx, 2][0]
                val normalizedHeight = detections[objectIdx, 3][0]
                val frameW = frame.cols()
                val frameH = frame.rows()
                val width = normalizedWidth * frameW
                val height = normalizedHeight * frameH
                val centerX = normalizedCenterX * frameW
                val centerY = normalizedCenterY * frameH
                val left = centerX - width / 2
                val top = centerY - height / 2
                classIds.add(classId)
                classScores.add(classScore.toFloat())
                boxes.add(Rect2d(left, top, width, height))
            }
        }

        if (classIds.isEmpty()) {
            return arrayListOf()
        }
        val matOfRect = MatOfRect2d().apply { fromList(boxes) }
        val matOfScores = MatOfFloat().apply { fromList(classScores) }
        val matOfIndexes = MatOfInt()
        Dnn.NMSBoxes(matOfRect, matOfScores, confThreshold, nmsThreshold, matOfIndexes)

        return matOfIndexes.toArray()
            .map { i -> ObjectDetectionResult(classIds[i], classScores[i], boxes[i]) }
    }

    companion object {
        private fun makeNet(cfgFile: String, darknetModel: String): Net {
            return Dnn.readNetFromDarknet(cfgFile, darknetModel).apply {
                setPreferableBackend(Dnn.DNN_BACKEND_OPENCV)
                setPreferableTarget(Dnn.DNN_TARGET_CPU)
                // setPreferableTarget(Dnn.DNN_TARGET_OPENCL)
            }
        }

        private fun makeNet(cfgBuffer: ByteArray, darknetBuffer: ByteArray): Net {
            val cfgBuffer = MatOfByte().apply { fromArray(*cfgBuffer) }
            val darknetBuffer = MatOfByte().apply { fromArray(*darknetBuffer) }
            return Dnn.readNetFromDarknet(cfgBuffer, darknetBuffer).apply {
                setPreferableBackend(Dnn.DNN_BACKEND_OPENCV)
                setPreferableTarget(Dnn.DNN_TARGET_CPU)
            }
        }

        private fun Size(width: Int, height: Int) = Size(width.toDouble(), height.toDouble())

        private fun Net.outputLayers(): List<String> {
            val layersNames = layerNames
            return unconnectedOutLayers.toArray().map { i -> layersNames[i - 1] }
        }

    }
}

