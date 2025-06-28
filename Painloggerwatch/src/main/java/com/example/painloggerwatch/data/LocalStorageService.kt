package com.example.painloggerwatch.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pain_entries")

class LocalStorageService(private val context: Context) {
    
    companion object {
        private val PENDING_ENTRIES_KEY = stringPreferencesKey("pending_entries")
    }
    
    /**
     * Save pain entry locally for later sync
     */
    suspend fun savePendingEntry(painEntry: PainEntry) {
        val currentEntries = getPendingEntries()
        val updatedEntries = currentEntries + painEntry
        
        context.dataStore.edit { preferences ->
            preferences[PENDING_ENTRIES_KEY] = Json.encodeToString(updatedEntries)
        }
    }
    
    /**
     * Get all pending entries that need to be synced
     */
    suspend fun getPendingEntries(): List<PainEntry> {
        return try {
            val entriesJson = context.dataStore.data.map { preferences ->
                preferences[PENDING_ENTRIES_KEY] ?: "[]"
            }
            
            var result: List<PainEntry> = emptyList()
            entriesJson.collect { json ->
                result = Json.decodeFromString(json)
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Clear all pending entries (after successful sync)
     */
    suspend fun clearPendingEntries() {
        context.dataStore.edit { preferences ->
            preferences[PENDING_ENTRIES_KEY] = "[]"
        }
    }
    
    /**
     * Remove specific entry after successful sync
     */
    suspend fun removePendingEntry(painEntry: PainEntry) {
        val currentEntries = getPendingEntries()
        val updatedEntries = currentEntries.filter { 
            it.timestamp != painEntry.timestamp 
        }
        
        context.dataStore.edit { preferences ->
            preferences[PENDING_ENTRIES_KEY] = Json.encodeToString(updatedEntries)
        }
    }
    
    /**
     * Get count of pending entries
     */
    fun getPendingEntriesCount(): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            try {
                val json = preferences[PENDING_ENTRIES_KEY] ?: "[]"
                val entries: List<PainEntry> = Json.decodeFromString(json)
                entries.size
            } catch (e: Exception) {
                0
            }
        }
    }
}