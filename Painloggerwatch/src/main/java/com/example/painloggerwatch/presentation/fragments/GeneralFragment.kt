package com.example.painloggerwatch.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.painlogger.PainLoggingWizardActivity
import com.example.painlogger.databinding.FragmentGeneralBinding
import com.example.painlogger.PainLoggingViewModel
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.launch
import com.example.painlogger.R

class GeneralFragment : Fragment() {

    private lateinit var viewModel: PainLoggingViewModel
    private lateinit var binding: FragmentGeneralBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGeneralBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity())[PainLoggingViewModel::class.java]

        // Observe previously saved general data and populate the UI if available
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.generalData.collect { generalData ->
                    generalData?.let {
                        if (binding.etPainLevel.text.isEmpty()) {
                            binding.etPainLevel.setText(it.painLevel)
                        }
                        if (binding.etTriggers.text.isEmpty()) {
                            binding.etTriggers.setText(it.triggers)
                        }
                    }
                }
            }
        }

        binding.btnContinue.setOnClickListener {
            val painLevel = binding.etPainLevel.text.toString().trim()
            val triggers = binding.etTriggers.text.toString().trim()
            viewModel.saveGeneral(painLevel, triggers)

            // Check for detailed_flow argument or intent extra
            val detailedFlow = arguments?.getBoolean("detailed_flow")
                ?: activity?.intent?.getBooleanExtra("detailed_flow", false) ?: false

            if (detailedFlow) {
                // Navigate to BodyPartsFragment for detailed logging
                parentFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, BodyPartsFragment())
                    .addToBackStack(null)
                    .commit()
            } else {
                // Normal wizard navigation
                (activity as? PainLoggingWizardActivity)?.navigateForward()
            }
        }

        return binding.root
    }

    fun saveGeneralData() {
        if (::binding.isInitialized) {
            val painLevel = binding.etPainLevel.text.toString().trim()
            val triggers = binding.etTriggers.text.toString().trim()
            viewModel.saveGeneral(painLevel, triggers)
        }
    }
}
