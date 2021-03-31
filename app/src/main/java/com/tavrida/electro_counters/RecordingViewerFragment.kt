package com.tavrida.electro_counters

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.tavrida.counter_scanner.utils.glob
import java.io.File

class RecordingViewerFragment(val sessionDir: File) : DialogFragment() {
    lateinit var imgView: ImageView
    lateinit var positionTxt: TextView
    lateinit var backBtn: Button
    lateinit var backFastBtn: Button
    lateinit var forwardBtn: Button
    lateinit var forwardFastBtn: Button
    lateinit var closeBtn: Button

    private val items =
        imageFiles(sessionDir).let { files -> ImagesNavigation(files, files.size / 2) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.recording_viewer, container, false)
        .also { bindUI(it) }
        .also { setupUI() }

    private fun bindUI(view: View) {
        imgView = view.findViewById(R.id.recorded_image_view)
        positionTxt = view.findViewById(R.id.position_txt)
        backBtn = view.findViewById(R.id.back_btn)
        backFastBtn = view.findViewById(R.id.backFast_btn)
        forwardBtn = view.findViewById(R.id.forward_btn)
        forwardFastBtn = view.findViewById(R.id.forwardFast_btn)

        closeBtn = view.findViewById(R.id.close_btn)
    }

    private fun setupUI() {
        closeBtn.setOnClickListener { close() }
        backBtn.setOnClickListener {
            if (items.move(-1)) {
                updateImageAndPosition()
            }
        }
        backFastBtn.setOnClickListener {
            if (items.move(-FAST_ITEMS)) {
                updateImageAndPosition()
            }
        }
        forwardBtn.setOnClickListener {
            if (items.move(1)) {
                updateImageAndPosition()
            }
        }
        forwardFastBtn.setOnClickListener {
            if (items.move(FAST_ITEMS)) {
                updateImageAndPosition()
            }
        }
        updateImageAndPosition()
    }

    private fun updateImageAndPosition() {
        imgView.setImageBitmap(items.currentImage)
        positionTxt.text = items.positionDesc
    }

    private fun close() {
        if (dialog != null) {
            dialog?.dismiss()
        } else {
            // activity?.supportFragmentManager?.popBackStackImmediate()
            // activity?.supportFragmentManager?.popBackStack()
            activity?.onBackPressed()
        }
    }

    private companion object {
        const val FAST_ITEMS = 30
        fun imageFiles(sessionDir: File) = glob(File(sessionDir, "*.jpg")).sorted()
    }
}

private class ImagesNavigation(val imageFiles: List<File>, navigateTo: Int = 0) {
    var currentImage: Bitmap? = null
        private set
    private var position = 0
    val positionDesc get() = "${position + 1}/${imageFiles.size}"

    init {
        move(navigateTo)
    }

    fun move(numOfItems: Int): Boolean {
        val newPos = clip(position + numOfItems, 0, imageFiles.size - 1)
        val stateChanged = newPos != position
        position = newPos
        if (stateChanged || currentImage == null) {
            currentImage = decode(imageFiles[position])
        }
        return stateChanged
    }

    private companion object {
        fun decode(path: File) = BitmapFactory.decodeFile(path.absolutePath)
        fun clip(value: Int, lower: Int, upper: Int): Int {
            if (value < lower)
                return lower
            if (value > upper)
                return upper
            return value
        }
    }


}
