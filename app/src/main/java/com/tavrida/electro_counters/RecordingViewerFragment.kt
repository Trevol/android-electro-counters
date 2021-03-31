package com.tavrida.electro_counters

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

    private val items = ImagesNavigation(imageFiles(sessionDir)).apply { setToMiddle() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.recording_viewer, container, false)

        imgView = view.findViewById(R.id.recorded_image_view)
        positionTxt = view.findViewById(R.id.position_txt)
        backBtn = view.findViewById(R.id.back_btn)
        backFastBtn = view.findViewById(R.id.backFast_btn)
        forwardBtn = view.findViewById(R.id.forward_btn)
        forwardFastBtn = view.findViewById(R.id.forwardFast_btn)

        closeBtn = view.findViewById(R.id.close_btn)
        closeBtn.setOnClickListener { close() }

        setupGallery()
        return view
    }

    private fun setupGallery() {
        TODO()
    }

    private fun loadFirstImage() = glob(File(sessionDir, "*.jpg"))
        .sorted().first()
        .let {
            BitmapFactory.decodeFile(it.absolutePath)
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
        fun imageFiles(sessionDir: File) = glob(File(sessionDir, "*.jpg")).sorted()
    }
}

private class ImagesNavigation(imageFiles: List<File>) {
    fun setToMiddle() {
        TODO("Not yet implemented")
    }
}
