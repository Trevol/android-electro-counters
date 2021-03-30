package com.tavrida.electro_counters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment

class RecordingViewerFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.recording_viewer, container, false)
        // view.findViewById<EditText>(R.id.username).text = SpannableStringBuilder(num.toString())
        view.findViewById<Button>(R.id.close_btn).setOnClickListener {
            if (dialog != null) {
                dialog?.dismiss()
            } else {
                // activity?.supportFragmentManager?.popBackStackImmediate()
                // activity?.supportFragmentManager?.popBackStack()
                activity?.onBackPressed()
            }
        }
        return view
    }

}