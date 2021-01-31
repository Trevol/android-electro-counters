package com.tavrida.counter_scanner.aggregation

import android.graphics.RectF
import com.tavrida.counter_scanner.utils.assert
import com.tavrida.utils.overlap

data class GroupBoxesResult(val groupIndices: Collection<Int>, val keptIndices: Collection<Int>)

fun groupBoxes(
    boxes: List<RectF>,
    scores: Collection<Float>?,
    overlap_threshold: Float
): GroupBoxesResult {
    (scores == null || (boxes.size == scores.size)).assert()
    (overlap_threshold >= 0).assert()

    data class IndexToGroup(val index: Int, val groupIndex: Int)

    val indicesToGroups = mutableListOf<IndexToGroup>()
    val keptIndices = mutableListOf<Int>()
    for (index in indicesByScore(boxes, scores)) {
        val (keep, maxOverlapIndex) = shouldKeepBox(index, boxes, keptIndices, overlap_threshold)
        if (keep) {
            keptIndices.add(index)
            indicesToGroups.add(IndexToGroup(index, index))
        } else {
            indicesToGroups.add(IndexToGroup(index, maxOverlapIndex))
        }
    }
    // restore original boxes order (sort by box index) and select groupIndex
    val groupIndices = indicesToGroups.sortedBy { it.index }.map { it.groupIndex }
    return GroupBoxesResult(groupIndices, keptIndices)
}

data class ShouldKeepBoxResult(val keep: Boolean, val maxOverlapIndex: Int)

private fun shouldKeepBox(
    currentBoxIndex: Int,
    boxes: List<RectF>,
    keptIndices: Collection<Int>,
    overlap_threshold: Float
): ShouldKeepBoxResult {
    val box = boxes[currentBoxIndex]
    var keep = true
    var maxOverlap = -1.0f
    var maxOverlapIndex = -1
    for (keptIndex in keptIndices) {
        val overlap = box.overlap(boxes[keptIndex])
        if (keep) {
            keep = overlap <= overlap_threshold
        }
        if (overlap > maxOverlap) {
            maxOverlap = overlap
            maxOverlapIndex = keptIndex
        }
    }
    return ShouldKeepBoxResult(keep, maxOverlapIndex)
}

private inline fun indicesByScore(boxes: Collection<RectF>, scores: Collection<Float>?) =
    scores?.mapIndexed { index, score -> index to score }?.sortedByDescending { it.second }
        ?.map { it.first }
        ?: boxes.mapIndexed { index, _ -> index }