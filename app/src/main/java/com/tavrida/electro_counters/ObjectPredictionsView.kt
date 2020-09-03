package com.tavrida.electro_counters

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.tavrida.ElectroCounters.detection.ObjectDetectionResult
import com.tavrida.utils.toDisplayStr
import com.tavrida.utils.toRectF
import kotlinx.android.synthetic.main.activity_camera.*
import org.opencv.core.Rect2d
import kotlin.random.Random

class ObjectPredictionsView : View {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) :
            super(context, attrs, defStyle)

    data class ObjectDetectionViewRect(val detection: ObjectDetectionResult, val viewRect: RectF)

    var detections: Collection<ObjectDetectionViewRect> = listOf()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (item in detections) {
            canvas.drawRect(item.viewRect, paint)
        }
    }

    fun showDetectionResult(detections: Collection<ObjectDetectionResult>) {
        if (detections.isEmpty() && this.detections.isEmpty()) {
            return
        }

        logDetectionResult(detections)
        this.detections = detections.map {
            val viewRect = it.normalizedBox.toRectF().toViewCoordinates()
            ObjectDetectionViewRect(it, viewRect)
        }
        postInvalidate()
    }

    private fun RectF.toViewCoordinates(): RectF {
        return mapOutputCoordinates(this)
    }

    private fun mapOutputCoordinates(location: RectF): RectF {
        val previewLocation = RectF(
            location.left * width,
            location.top * height,
            location.right * width,
            location.bottom * height
        )
        // Step 2: compensate for 1:1 to 4:3 aspect ratio conversion + small margin
        val margin = 0.1f
        val requestedRatio = 4f / 3f
        val midX = (previewLocation.left + previewLocation.right) / 2f
        val midY = (previewLocation.top + previewLocation.bottom) / 2f
        return if (width < height) {
            RectF(
                midX - (1f + margin) * requestedRatio * previewLocation.width() / 2f,
                midY - (1f - margin) * previewLocation.height() / 2f,
                midX + (1f + margin) * requestedRatio * previewLocation.width() / 2f,
                midY + (1f - margin) * previewLocation.height() / 2f
            )
        } else {
            RectF(
                midX - (1f - margin) * previewLocation.width() / 2f,
                midY - (1f + margin) * requestedRatio * previewLocation.height() / 2f,
                midX + (1f - margin) * previewLocation.width() / 2f,
                midY + (1f + margin) * requestedRatio * previewLocation.height() / 2f
            )
        }
    }

    companion object {
        val paint = Paint().apply {
            color = Color.argb(255, 255, 0, 0)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }


        private fun logDetectionResult(detections: Collection<ObjectDetectionResult>) {
            if (detections.isNotEmpty()) {
                val tag = "DETECTIONS"
                Log.d(tag, "=============================")
                for (p in detections) {
                    Log.d(tag, "${p.classId} ${p.classScore} ${p.box.toDisplayStr()}")
                }
            }
        }
    }
}
