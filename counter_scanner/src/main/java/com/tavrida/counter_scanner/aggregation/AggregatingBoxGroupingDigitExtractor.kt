package com.tavrida.counter_scanner.aggregation

import com.tavrida.counter_scanner.detection.DigitDetectionResult

data class Digits_AggregatedDetections(
    val digitsAtBoxes: List<DigitAtBox>,
    val aggregatedDetections: List<AggregatedDetections>
)

class AggregatingBoxGroupingDigitExtractor {
    fun extract(currentDetections: Collection<DigitDetectionResult>, prevDetections: Collection<AggregatedDetections>):
            Digits_AggregatedDetections {

        val boxes = currentDetections.map { it.boxInImage } +
                prevDetections.map { it.box }
        val scores = currentDetections.map { it.score } + prevDetections.map { it.score }
        val digitsCounts = currentDetections.map { listOf(DigitCount(it.digit, 1)) } +
                prevDetections.map { it.digitsCounts }

        val aggregatedDetections = groupBoxes(boxes, scores, .04f)
            .groupIndices.zip(digitsCounts)
            .groupBy({ it.first }, { it.second })
            .map { index, digitsCountsByBox ->
                AggregatedDetections(boxes[index], scores[index], merge(digitsCountsByBox))
            }

        val digits = aggregatedDetections.filter { it.totalCount() >= minBoxesInGroup }
            .map { DigitAtBox(it.digitWithMaxCount(), it.box) }

        return Digits_AggregatedDetections(digits, aggregatedDetections)
    }

    companion object {
        const val minBoxesInGroup = 3

        inline fun merge(digitsCountsByBox: List<List<DigitCount>>) =
            digitsCountsByBox.flatten()
                .groupBy({ it.digit }, { it.count })
                .map { digit, digitCounts -> DigitCount(digit, digitCounts.sum()) }


        inline fun AggregatedDetections.totalCount() = digitsCounts.sumBy { it.count }
        inline fun AggregatedDetections.digitWithMaxCount() = digitsCounts.maxBy { it.count }!!.digit

        inline fun <K, V, R> Map<out K, V>.map(transform: (K, V) -> R) =
            this.map { entry -> transform(entry.key, entry.value) }
    }
}


