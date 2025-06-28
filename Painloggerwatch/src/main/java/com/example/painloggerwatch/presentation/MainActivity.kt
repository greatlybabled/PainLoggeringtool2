package com.example.painloggerwatch.presentation

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.*
import com.example.painloggerwatch.data.DataSyncService
import com.example.painloggerwatch.data.DetailedPainEntry
import com.example.painloggerwatch.data.LocalStorageService
import com.example.painloggerwatch.data.PainEntry
import com.example.painloggerwatch.presentation.theme.PainLoggerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private lateinit var dataSyncService: DataSyncService
    private lateinit var localStorageService: LocalStorageService
    
    companion object {
        private const val TAG = "WatchMainActivity"
        private val BODY_PARTS = listOf(
            "Head", "Neck", "Shoulders", "Arms", "Chest", 
            "Back", "Abdomen", "Hips", "Legs", "Feet"
        )
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        dataSyncService = DataSyncService(this)
        localStorageService = LocalStorageService(this)
        
        setTheme(android.R.style.Theme_DeviceDefault)
        
        setContent {
            PainLoggerTheme {
                PainLoggingScreen()
            }
        }
    }

    @Composable
    fun PainLoggingScreen() {
        var currentStep by remember { mutableStateOf(0) }
        var generalPainLevel by remember { mutableStateOf(0) }
        var detailedEntries by remember { mutableStateOf(listOf<DetailedPainEntry>()) }
        var notes by remember { mutableStateOf("") }
        var isLogging by remember { mutableStateOf(false) }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (currentStep) {
                0 -> GeneralPainScreen(
                    painLevel = generalPainLevel,
                    onPainLevelChange = { generalPainLevel = it },
                    onNext = { currentStep = 1 }
                )
                1 -> DetailedPainScreen(
                    entries = detailedEntries,
                    onEntriesChange = { detailedEntries = it },
                    onNext = { currentStep = 2 },
                    onBack = { currentStep = 0 }
                )
                2 -> NotesScreen(
                    notes = notes,
                    onNotesChange = { notes = it },
                    onSubmit = {
                        if (!isLogging) {
                            isLogging = true
                            logPainEntry(generalPainLevel, detailedEntries, notes) {
                                isLogging = false
                                // Reset form
                                currentStep = 0
                                generalPainLevel = 0
                                detailedEntries = emptyList()
                                notes = ""
                            }
                        }
                    },
                    onBack = { currentStep = 1 },
                    isLogging = isLogging
                )
            }
        }
    }
    
    @Composable
    fun GeneralPainScreen(
        painLevel: Int,
        onPainLevelChange: (Int) -> Unit,
        onNext: () -> Unit
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Pain Level",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = painLevel.toString(),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            
            // Two rows of buttons for better fit on watch
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in 1..5) {
                        Button(
                            onClick = { onPainLevelChange(i) },
                            modifier = Modifier.size(32.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (i == painLevel) MaterialTheme.colors.primary 
                                                else MaterialTheme.colors.surface
                            )
                        ) {
                            Text(
                                text = i.toString(),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in 6..10) {
                        Button(
                            onClick = { onPainLevelChange(i) },
                            modifier = Modifier.size(32.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (i == painLevel) MaterialTheme.colors.primary 
                                                else MaterialTheme.colors.surface
                            )
                        ) {
                            Text(
                                text = i.toString(),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onNext,
                enabled = painLevel > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Next")
            }
        }
    }
    
    @Composable
    fun DetailedPainScreen(
        entries: List<DetailedPainEntry>,
        onEntriesChange: (List<DetailedPainEntry>) -> Unit,
        onNext: () -> Unit,
        onBack: () -> Unit
    ) {
        Column {
            Text(
                text = "Body Parts",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(BODY_PARTS) { bodyPart ->
                    val existingEntry = entries.find { it.bodyPart == bodyPart }
                    val intensity = existingEntry?.intensity ?: 0
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = bodyPart,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            for (i in 1..5) {
                                Button(
                                    onClick = {
                                        val newEntries = entries.filter { it.bodyPart != bodyPart }
                                        if (i != intensity) {
                                            onEntriesChange(newEntries + DetailedPainEntry(bodyPart, i))
                                        } else {
                                            onEntriesChange(newEntries)
                                        }
                                    },
                                    modifier = Modifier.size(20.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = if (i == intensity) MaterialTheme.colors.primary 
                                                        else MaterialTheme.colors.surface
                                    )
                                ) {
                                    Text(
                                        text = i.toString(),
                                        fontSize = 8.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = onBack) {
                    Text("Back")
                }
                Button(onClick = onNext) {
                    Text("Next")
                }
            }
        }
    }
    
    @Composable
    fun NotesScreen(
        notes: String,
        onNotesChange: (String) -> Unit,
        onSubmit: () -> Unit,
        onBack: () -> Unit,
        isLogging: Boolean
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Notes (Optional)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            // Simple text display for notes on watch (editing would be difficult)
            Text(
                text = if (notes.isEmpty()) "No notes" else notes,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = onBack) {
                    Text("Back")
                }
                Button(
                    onClick = onSubmit,
                    enabled = !isLogging
                ) {
                    if (isLogging) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colors.onPrimary
                        )
                    } else {
                        Text("Log Pain")
                    }
                }
            }
        }
    }
    
    private fun logPainEntry(
        generalPainLevel: Int,
        detailedEntries: List<DetailedPainEntry>,
        notes: String,
        onComplete: () -> Unit
    ) {
        lifecycleScope.launch {
            try {
                val painEntry = PainEntry(
                    timestamp = System.currentTimeMillis(),
                    generalPainLevel = generalPainLevel,
                    detailedEntries = detailedEntries,
                    notes = notes,
                    deviceSource = "watch"
                )
                
                // Try to sync to phone first
                val syncSuccess = dataSyncService.syncPainEntryToPhone(painEntry)
                
                if (syncSuccess) {
                    Toast.makeText(this@MainActivity, "Pain logged and synced to phone", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Pain entry synced successfully")
                } else {
                    // If sync fails, save locally
                    localStorageService.savePendingEntry(painEntry)
                    Toast.makeText(this@MainActivity, "Pain logged locally (will sync when phone is available)", Toast.LENGTH_LONG).show()
                    Log.d(TAG, "Pain entry saved locally for later sync")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error logging pain entry", e)
                Toast.makeText(this@MainActivity, "Error logging pain entry", Toast.LENGTH_SHORT).show()
            } finally {
                onComplete()
            }
        }
    }
}