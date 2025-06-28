package com.example.painloggerwatch.presentation // Assuming the data package for preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// Keep this OUTSIDE the class and within the correct package
// Creates a DataStore instance for theme settings
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_settings")

// Class to manage theme preferences using DataStore
class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Get the DataStore instance
    private val dataStore = context.dataStore

    // Define the key for the dark theme preference
    companion object {
        private val DARK_THEME_KEY = booleanPreferencesKey("dark_theme_enabled")
    }

    // Flow to observe theme preference changes
    val themeFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            // Map DataStore preference to a Boolean (defaulting to false)
            preferences[DARK_THEME_KEY] ?: false
        }

    // Suspend function to update the theme preference
    suspend fun updateThemePreference(isDarkTheme: Boolean) {
        dataStore.edit { preferences ->
            preferences[DARK_THEME_KEY] = isDarkTheme
        }
    }
}
