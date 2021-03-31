package com.tavrida.counter_scanner

import com.tavrida.counter_scanner.detection.DigitDetectionResult
import com.tavrida.counter_scanner.detection.ScreenDigitDetectionResult

interface TelemetryRecorder {
    fun record(
        frameId: Int,
        rawDetection: ScreenDigitDetectionResult,
        finalDigitsDetections: List<DigitDetectionResult>,
        detectionMs: Long,
        trackingMs: Long
    )
}