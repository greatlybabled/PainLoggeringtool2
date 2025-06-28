package com.example.painlogger.data

import android.util.Log
import com.google.android.gms.wearable.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class WearDataListenerService : WearableListenerService() {
    
    companion object {
        private const val TAG = "WearDataListener"
        private const val PAIN_ENTRY_PATH = "/pain_entry"
        private const val SYNC_REQUEST_PATH = "/sync_request"
    }
    
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val item = event.dataItem
                if (item.uri.path?.compareTo(PAIN_ENTRY_PATH) == 0) {
                    val dataMap = DataMapItem.fromDataItem(item).dataMap
                    val painEntryJson = dataMap.getString("pain_entry_json")
                    
                    if (painEntryJson != null) {
                        processPainEntryFromWatch(painEntryJson)
                    }
                }
            }
        }
    }
    
    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        
        when (messageEvent.path) {
            SYNC_REQUEST_PATH -> {
                // Handle sync status request from watch
                Log.d(TAG, "Sync status request received from watch")
                // You can implement status response here if needed
            }
        }
    }
    
    private fun processPainEntryFromWatch(painEntryJson: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val painEntry = Json.decodeFromString<WatchPainEntry>(painEntryJson)
                Log.d(TAG, "Received pain entry from watch: $painEntry")
                
                // Save to CSV files
                savePainEntryToCSV(painEntry)
                
                Log.d(TAG, "Pain entry from watch saved successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing pain entry from watch", e)
            }
        }
    }
    
    private suspend fun savePainEntryToCSV(painEntry: WatchPainEntry) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val timestamp = dateFormat.format(Date(painEntry.timestamp))
            
            // Save to general pain log
            saveToGeneralCSV(timestamp, painEntry.generalPainLevel, painEntry.notes, painEntry.deviceSource)
            
            // Save detailed entries if any
            if (painEntry.detailedEntries.isNotEmpty()) {
                saveToDetailedCSV(timestamp, painEntry.detailedEntries, painEntry.deviceSource)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error saving pain entry to CSV", e)
            throw e
        }
    }
    
    private suspend fun saveToGeneralCSV(timestamp: String, painLevel: Int, notes: String, deviceSource: String) {
        // This should use the same CSV file that the phone app uses
        // You'll need to get the file URI from SharedPreferences or similar
        val csvLine = "$timestamp,$painLevel,\"$notes\",$deviceSource\n"
        
        // For now, log the CSV line - you'll need to implement actual file writing
        // based on how your phone app currently handles CSV files
        Log.d(TAG, "General CSV line: $csvLine")
        
        // TODO: Implement actual CSV file writing using the same mechanism as the phone app
        // This might involve:
        // 1. Getting the file URI from SharedPreferences
        // 2. Using ContentResolver to write to the file
        // 3. Or using the same CSV writing service that the phone app uses
    }
    
    private suspend fun saveToDetailedCSV(timestamp: String, detailedEntries: List<WatchDetailedPainEntry>, deviceSource: String) {
        for (entry in detailedEntries) {
            val csvLine = "$timestamp,${entry.bodyPart},${entry.intensity},\"${entry.notes}\",$deviceSource\n"
            Log.d(TAG, "Detailed CSV line: $csvLine")
            
            // TODO: Implement actual CSV file writing
        }
    }
}

// Data classes for receiving data from watch
@kotlinx.serialization.Serializable
data class WatchPainEntry(
    val timestamp: Long,
    val generalPainLevel: Int,
    val detailedEntries: List<WatchDetailedPainEntry> = emptyList(),
    val notes: String = "",
    val deviceSource: String = "watch"
)

@kotlinx.serialization.Serializable
data class WatchDetailedPainEntry(
    val bodyPart: String,
    val intensity: Int = 0,
    val notes: String = ""
)