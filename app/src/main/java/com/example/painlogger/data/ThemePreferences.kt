package com.example.painlogger.data

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

// Keep this OUTSIDE the class
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_settings")

class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        private val DARK_THEME_KEY = booleanPreferencesKey("dark_theme_enabled")
    }

    val themeFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[DARK_THEME_KEY] ?: false
        }

    suspend fun updateThemePreference(isDarkTheme: Boolean) {
        dataStore.edit { preferences ->
            preferences[DARK_THEME_KEY] = isDarkTheme
        }
    }
}