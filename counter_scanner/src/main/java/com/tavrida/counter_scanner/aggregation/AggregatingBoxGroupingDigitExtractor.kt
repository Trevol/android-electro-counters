package com.tavrida.counter_scanner.aggregation

import com.tavrida.counter_scanner.detection.DigitDetectionResult

class AggregatingBoxGroupingDigitExtractor {
    /*
    data class Digits_AggregatedDetections(
    val digitsAtLocations: List<DigitAtLocation>,
    val aggregatedDetections: List<AggregatedDetections>)
    fun extract(currentDetections: Collection<DigitDetectionResult>, prevDetections: Collection<AggregatedDetections>):
            Digits_AggregatedDetections {

        val aggregatedDetections = aggregateDetections(currentDetections, prevDetections)
        val digits = extractDigits(aggregatedDetections)
        return Digits_AggregatedDetections(digits, aggregatedDetections)
    }*/

    fun aggregateDetections(
        currentDetections: Collection<DigitDetectionResult>,
        prevDetections: Collection<AggregatedDetections>
    ): List<AggregatedDetections> {
        val boxes = currentDetections.map { it.location } +
                prevDetections.map { it.location }
        val scores = currentDetections.map { it.score } + prevDetections.map { it.score }
        val digitsCounts = currentDetections.map { listOf(DigitCount(it.digit, 1)) } +
                prevDetections.map { it.digitsCounts }
        //TODO: boxes from detections have priority - should be in keptIndices
        return groupBoxes(boxes, scores, .04f)
            .groupIndices.zip(digitsCounts)
            .groupBy({ it.first }, { it.second })
            .map { index, digitsCountsByBox ->
                AggregatedDetections(boxes[index], scores[index], merge(digitsCountsByBox))
            }
    }

    fun extractDigits(detections: Collection<AggregatedDetections>) = detections
        .filter { it.totalCount >= minBoxesInGroup }
        .map { DigitAtLocation(it.digitWithMaxCount, it.location) }

    companion object {
        const val minBoxesInGroup = 3

        inline fun merge(digitsCountsByBox: List<List<DigitCount>>) =
            digitsCountsByBox.flatten()
                .groupBy({ it.digit }, { it.count })
                .map { digit, digitCounts -> DigitCount(digit, digitCounts.sum()) }


        inline fun <K, V, R> Map<out K, V>.map(transform: (K, V) -> R) =
            this.map { entry -> transform(entry.key, entry.value) }
    }
}


