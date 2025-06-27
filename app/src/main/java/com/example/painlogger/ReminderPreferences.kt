package com.example.painlogger // Assuming the data package for preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// Keep this OUTSIDE the class and within the correct package
// Creates a DataStore instance for reminder settings
val Context.reminderDataStore: DataStore<Preferences> by preferencesDataStore(name = "reminder_settings")

// Class to manage reminder settings using DataStore
class ReminderPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Get the DataStore instance
    private val dataStore = context.reminderDataStore

    // Define keys for the preferences
    companion object {
        private val REMINDER_TYPE_KEY = stringPreferencesKey("reminder_type")
        private val INTERVAL_MINUTES_KEY = longPreferencesKey("interval_minutes")
        private val SPECIFIC_HOUR_KEY = intPreferencesKey("specific_hour")
        private val SPECIFIC_MINUTE_KEY = intPreferencesKey("specific_minute")
    }

    // Flow to observe reminder settings changes
    val reminderSettings: Flow<ReminderSettings> = dataStore.data
        .map { preferences ->
            // Map DataStore preferences to ReminderSettings data class
            ReminderSettings(
                type = preferences[REMINDER_TYPE_KEY] ?: "interval", // Default to interval
                intervalMinutes = preferences[INTERVAL_MINUTES_KEY] ?: 60L, // Default to 60 minutes
                specificHour = preferences[SPECIFIC_HOUR_KEY] ?: 8, // Default to 8 AM
                specificMinute = preferences[SPECIFIC_MINUTE_KEY] ?: 0 // Default to 0 minutes
            )
        }

    // Suspend function to update the reminder type
    suspend fun updateReminderType(type: String) {
        dataStore.edit { preferences ->
            preferences[REMINDER_TYPE_KEY] = type
        }
    }

    // Suspend function to update the interval in minutes
    suspend fun updateInterval(intervalMinutes: Long) {
        dataStore.edit { preferences ->
            preferences[INTERVAL_MINUTES_KEY] = intervalMinutes
        }
    }

    // Suspend function to update the specific time (hour and minute)
    suspend fun updateSpecificTime(hour: Int, minute: Int) {
        dataStore.edit { preferences ->
            preferences[SPECIFIC_HOUR_KEY] = hour
            preferences[SPECIFIC_MINUTE_KEY] = minute
        }
    }

    // Data class to represent the reminder settings
    data class ReminderSettings(
        val type: String,
        val intervalMinutes: Long,
        val specificHour: Int,
        val specificMinute: Int
    )
}
