package com.tavrida.counter_scanner.detection.tflite

/*
private object roi {
        val w: Int = 400
        val h: Int = 180

        val roiPaint = Paint().apply {
            color = Color.rgb(0, 255, 0)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        fun roiBitmap(src: Bitmap): Pair<Bitmap, Rect> {
            val r = rect(src)
            return Bitmap.createBitmap(src, r.left, r.top, r.width(), r.height()) to r
        }

        inline fun rect(src: Bitmap): Rect {
            val centerX = src.width / 2.0f
            val centerY = src.height / 2.0f

            val halfW = w / 2f
            val halfH = h / 2f
            return Rect(
                (centerX - halfW).toInt(),
                (centerY - halfH).toInt(),
                (centerX + halfW).toInt(),
                (centerY + halfH).toInt()
            )
        }

        fun draw(img: Bitmap): Bitmap {
            val r = rect(img)
            Canvas(img).drawRect(r, roiPaint)
            return img
        }
    }

    var prev = System.currentTimeMillis()

    @SuppressLint("UnsafeExperimentalUsageError")
    fun analyzeImage(image: ImageProxy) {
        val inputBitmap = image.use {
            getBitmapBuffer(image.width, image.height)
                .apply { yuvToRgbConverter.yuvToRgb(image.image!!, this) }
                .compensateSensorRotation(image.imageInfo.rotationDegrees)
        }
        val current = System.currentTimeMillis()
        (current - prev).log2()
        prev = current

        val (roiImage, roiRect) = roi.roiBitmap(inputBitmap)
        val detections = detector.detect(roiImage, .2f)

        imageView_preview.post {
            val imageWithRoi = roi.draw(inputBitmap.copy())
            vizUtils.drawDetections(imageWithRoi, detections, roiRect)

            if (started) {
                framesStorage.addFrame(imageWithRoi)
            }
            imageView_preview.setImageBitmap(imageWithRoi)
        }

    }

    private object vizUtils {
        const val screenId = 11
        private val screenPaint = Paint(Color.rgb(0, 0, 255), strokeWidth = 2f)
        private val digitBoxPaint = Paint(Color.rgb(253, 212, 81), strokeWidth = 2f)
        private val digitPaint =
            Paint(
                Color.rgb(253, 212, 81), style = Paint.Style.FILL_AND_STROKE,
                strokeWidth = 1f, textSize = 20f
            )

        private inline fun paint(classId: Int) =
            if (classId == screenId) screenPaint else digitBoxPaint

        private inline fun TfliteDetector.ObjectDetection.isDigit() = classId != screenId

        private fun Paint(
            color: Int,
            style: Paint.Style = Paint.Style.STROKE,
            strokeWidth: Float = 1f,
            textSize: Float = 15f
        ) = Paint().apply {
            this.color = color
            this.style = style
            this.strokeWidth = strokeWidth
            this.textSize = textSize
        }

        private fun remap(src: RectF, x: Int, y: Int) = RectF(
            src.left + x,
            src.top + y,
            src.right + x,
            src.bottom + y
        )

        fun drawDetections(
            srcBmp: Bitmap,
            detections: List<TfliteDetector.ObjectDetection>,
            roiRect: Rect
        ) {
            val canvas = Canvas(srcBmp)
            for (d in detections) {
                val remappedRect = remap(d.location, roiRect.left, roiRect.top)
                canvas.drawRect(remappedRect, paint(d.classId))
                if (d.isDigit()) {
                    canvas.drawText(
                        (d.classId - 1).toString(),
                        remappedRect.left + 2,
                        remappedRect.top - 2,
                        digitPaint
                    )
                }
            }
        }
    }
* */