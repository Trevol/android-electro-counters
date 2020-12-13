package com.tavrida.counter_scanner

import com.tavrida.counter_scanner.aggregation.AggregatingBoxGroupingDigitExtractor
import com.tavrida.counter_scanner.detection.DigitDetectionResult
import com.tavrida.counter_scanner.detection.TwoStageDigitsDetector
import com.tavrida.electro_counters.tracking.AggregatedDigitDetectionTracker
import com.tavrida.counter_scanner.aggregation.AggregatedDetections
import com.tavrida.counter_scanner.aggregation.DigitAtBox
import com.tavrida.counter_scanner.utils.rgb2gray
import org.opencv.core.Mat

class CounterReadingScanner(val detector: TwoStageDigitsDetector) {
    private var prevGrayImg: Mat? = null
    val digitExtractor = AggregatingBoxGroupingDigitExtractor()
    val digitDetectionTracker = AggregatedDigitDetectionTracker()
    var aggregatedDetections = listOf<AggregatedDetections>()

    fun scan(rgbImg: Mat): ScanResult {
        val currentDetections = detector.detect(rgbImg)?.digitsDetections ?: listOf()
        val grayImg = rgbImg.rgb2gray()
        if (prevGrayImg != null) {
            aggregatedDetections = digitDetectionTracker.track(prevGrayImg!!, grayImg, aggregatedDetections)
        }
        prevGrayImg = grayImg

        val extractionResult = digitExtractor.extract(currentDetections, aggregatedDetections)
        aggregatedDetections = extractionResult.aggregatedDetections
        return ScanResult(currentDetections, extractionResult.digitsAtBoxes, aggregatedDetections)
    }
}

data class ScanResult(
    val currentDetections: List<DigitDetectionResult>,
    val digitsAtBox: List<DigitAtBox>,
    val aggregatedDetections: List<AggregatedDetections>
)