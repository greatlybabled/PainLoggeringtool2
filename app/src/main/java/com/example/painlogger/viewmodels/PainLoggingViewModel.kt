package com.example.painlogger // Make sure this is your correct package

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update

// Import the DetailedPainEntry data class
import com.example.painlogger.DetailedPainEntry

class PainLoggingViewModel : ViewModel() {
    // StateFlow to hold the overall pain level and general triggers
    private val _generalData = MutableStateFlow<GeneralData?>(null)
    // StateFlow to hold the specific triggers description
    private val _triggersData = MutableStateFlow<String?>(null)
    // StateFlow to hold the list of detailed pain entries for selected body parts
    private val _detailedPainEntries = MutableStateFlow<List<DetailedPainEntry>>(emptyList())

    // Expose the data as StateFlows for UI observation
    val generalData = _generalData.asStateFlow()
    val triggersData = _triggersData.asStateFlow()
    val detailedPainEntries = _detailedPainEntries.asStateFlow()

    // Function to save general pain data (level and triggers)
    fun saveGeneral(painLevel: String, triggers: String) {
        viewModelScope.launch {
            _generalData.value = GeneralData(painLevel, triggers)
        }
    }

    // Function to save the specific triggers description
    fun saveTriggers(triggers: String) {
        viewModelScope.launch {
            _triggersData.value = triggers
        }
    }

    // Added a new function to save a list of DetailedPainEntry
    fun saveDetailedPainEntries(entries: List<DetailedPainEntry>) {
        viewModelScope.launch {
            _detailedPainEntries.update { entries }
        }
    }
    fun updateDetailedPainEntries(newEntries: List<DetailedPainEntry>){
        viewModelScope.launch {
            _detailedPainEntries.update { newEntries }
        }
    }

    // Data class to represent the overall pain data
    data class GeneralData(val painLevel: String, val triggers: String)
    // Keep your existing data classes here if any
}