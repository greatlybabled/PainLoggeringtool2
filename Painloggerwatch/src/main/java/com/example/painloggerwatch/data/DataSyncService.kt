package com.example.painloggerwatch.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.*
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DataSyncService(private val context: Context) {
    
    private val dataClient: DataClient by lazy { Wearable.getDataClient(context) }
    private val messageClient: MessageClient by lazy { Wearable.getMessageClient(context) }
    
    companion object {
        private const val TAG = "DataSyncService"
        private const val PAIN_ENTRY_PATH = "/pain_entry"
        private const val SYNC_REQUEST_PATH = "/sync_request"
    }
    
    /**
     * Send pain entry data to the phone
     */
    suspend fun syncPainEntryToPhone(painEntry: PainEntry): Boolean {
        return try {
            val json = Json.encodeToString(painEntry)
            val putDataRequest = PutDataMapRequest.create(PAIN_ENTRY_PATH).apply {
                dataMap.putString("pain_entry_json", json)
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }.asPutDataRequest()
            
            putDataRequest.setUrgent()
            
            val result = dataClient.putDataItem(putDataRequest).await()
            Log.d(TAG, "Pain entry synced to phone: ${result.uri}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync pain entry to phone", e)
            false
        }
    }
    
    /**
     * Request sync status from phone
     */
    suspend fun requestSyncStatus(): Boolean {
        return try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            for (node in nodes) {
                messageClient.sendMessage(
                    node.id,
                    SYNC_REQUEST_PATH,
                    "status_request".toByteArray()
                ).await()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request sync status", e)
            false
        }
    }
    
    /**
     * Check if phone is connected
     */
    suspend fun isPhoneConnected(): Boolean {
        return try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            nodes.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check phone connection", e)
            false
        }
    }
}