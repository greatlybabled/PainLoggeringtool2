package com.example.painloggerwatch.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.painlogger.PainLoggingViewModel
import com.example.painlogger.adapters.BodyPartDetailedAdapter
import com.example.painlogger.databinding.FragmentBodyPartsBinding
import kotlinx.coroutines.launch

class BodyPartsFragment : Fragment() {

    private var _binding: FragmentBodyPartsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PainLoggingViewModel by activityViewModels()
    private lateinit var bodyPartAdapter: BodyPartDetailedAdapter

    private val bodyParts = listOf(
        "HD: Head - General", "FRN: Forehead", "TMP-L: Temple (Left)", "TMP-R: Temple (Right)",
        "TOP: Top of Head (Parietal)", "OCC-GEN: Occiput - General",
        "OCC-LOW: Occiput - Lower / Base of Skull", "OCC-UP: Occiput - Upper",
        "OCC-SIDE-L: Occiput - Side (Left)", "OCC-SIDE-R: Occiput - Side (Right)",
        "BHE-L: Behind Ear (Left)", "BHE-R: Behind Ear (Right)", "EYE-L: Eye (Left)",
        "EYE-R: Eye (Right)", "EAR-L: Ear (Left)", "EAR-R: Ear (Right)", "NOS: Nose",
        "MOU: Mouth", "JAW-L: Jaw (Left)", "JAW-R: Jaw (Right)",
        "NEC-FRN: Neck - Front", "NEC-BAC: Neck - Back",
        "NEC-SIDE-L: Neck - Side (Left)", "NEC-SIDE-R: Neck - Side (Right)",
        "SHL-L: Shoulder (Left)", "SHL-R: Shoulder (Right)", "UAR-L: Upper Arm (Left)",
        "UAR-R: Upper Arm (Right)", "ELB-L: Elbow (Left)", "ELB-R: Elbow (Right)",
        "FAR-L: Forearm (Left)", "FAR-R: Forearm (Right)", "WRI-L: Wrist (Left)",
        "WRI-R: Wrist (Right)", "HAN-L: Hand (Left)", "HAN-R: Hand (Right)",
        "FNG-L: Fingers (Left)", "FNG-R: Fingers (Right)", "CHE: Chest",
        "BAC-UP: Upper Back", "BAC-MID: Middle Back", "BAC-LOW: Lower Back",
        "ABD: Abdomen", "PEL: Pelvis", "HIP-L: Hip (Left)", "HIP-R: Hip (Right)",
        "GRO: Groin", "THI-L: Thigh (Left)", "THI-R: Thigh (Right)", "KNE-L: Knee (Left)",
        "KNE-R: Knee (Right)", "CAL-L: Calf (Left)", "CAL-R: Calf (Right)",
        "ANK-L: Ankle (Left)", "ANK-R: Ankle (Right)", "FOO-L: Foot (Left)",
        "FOO-R: Foot (Right)", "TOE-L: Toes (Left)", "TOE-R: Toes (Right)",
        "ARCH-L: Arch of Foot (Left)", "ARCH-R: Arch of Foot (Right)",
        "HEEL-L: Heel (Left)", "HEEL-R: Heel (Right)", "SKN: Skin", "MUS: Muscle",
        "JNT: Joint", "BON: Bone", "NER: Nerve", "BLO: Blood Vessel", "LYM: Lymph Node",
        "ORG: Organ", "OTH: Other", "GEN: General"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBodyPartsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
    }

    private fun setupRecyclerView() {
        bodyPartAdapter = BodyPartDetailedAdapter(bodyParts).apply {
            onIntensityChanged = { entries ->
                viewModel.updateDetailedPainEntries(entries)
            }
        }

        binding.rvBodyParts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bodyPartAdapter
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.detailedPainEntries.collect { entries ->
                    bodyPartAdapter.updateEntries(entries)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun saveBodyPartsData() {
        // Data is automatically saved through the intensityChanged listener
    }
}