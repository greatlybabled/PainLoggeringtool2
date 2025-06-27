
package com.example.painlogger.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.painlogger.MainActivity
import com.example.painlogger.R

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val startLoggingButton = view.findViewById<Button>(R.id.startLoggingButton)
        val testWorkerButton = view.findViewById<Button>(R.id.testWorkerButton)

        startLoggingButton.setOnClickListener {
            // Call the method in MainActivity to show the logging type dialog
            (requireActivity() as? MainActivity)?.showLoggingTypeDialog()
        }

        testWorkerButton.setOnClickListener {
            // TODO: Test worker logic
        }
    }
}
