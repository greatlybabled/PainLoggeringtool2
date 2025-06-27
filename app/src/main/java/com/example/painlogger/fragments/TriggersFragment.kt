package com.example.painlogger.fragments // Adjust package name if needed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.semantics.text
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.painlogger.PainLoggingWizardActivity
import com.example.painlogger.PainLoggingViewModel
import com.example.painlogger.databinding.FragmentTriggersBinding
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.launch

// Fragment for the third step of the pain logging wizard (Triggers)
class TriggersFragment : Fragment() {

    private lateinit var viewModel: PainLoggingViewModel
    private lateinit var binding: FragmentTriggersBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTriggersBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity())[PainLoggingViewModel::class.java]

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.triggersData.collect { triggers ->
                    triggers?.let {
                        if (binding.etTriggers.text.isEmpty()) {
                            binding.etTriggers.setText(it)
                        }
                    }
                }
            }
        }

        return binding.root
    }

    fun saveTriggersData() {
        if (::binding.isInitialized) {
            val triggers = binding.etTriggers.text.toString()
            viewModel.saveTriggers(triggers)
        }
    }
}