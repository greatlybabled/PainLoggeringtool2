package com.example.painlogger.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.painlogger.ReminderRepository
import com.example.painlogger.Reminder
import com.example.painlogger.ReminderCategory
import com.example.painlogger.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val workScheduler: WorkScheduler
) : ViewModel() {

    // Flow of reminders filtered by category
    val detailedReminders: Flow<List<Reminder>> =
        reminderRepository.getRemindersByCategory(ReminderCategory.DETAILED)

    val generalReminders: Flow<List<Reminder>> =
        reminderRepository.getRemindersByCategory(ReminderCategory.GENERAL)

    /**
     * Saves or updates a reminder and schedules its associated work
     * @param reminder The reminder to save/update
     */
    fun saveReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderRepository.upsertReminder(reminder)
            // Only schedule if the reminder has a config
            reminder.config?.let { config ->
                workScheduler.scheduleReminder(config)
            }
        }
    }

    /**
     * Deletes a reminder and cancels its associated work
     * @param reminder The reminder to delete
     */
    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderRepository.deleteReminder(reminder.id)
            workScheduler.cancelReminder(reminder.id.toString())
        }
    }

    /**
     * Gets a specific reminder by its ID
     * @param id The UUID of the reminder to retrieve
     * @return The reminder if found, null otherwise
     */
    suspend fun getReminderById(id: UUID): Reminder? {
        return reminderRepository.getReminderByIdDirect(id)
    }
}