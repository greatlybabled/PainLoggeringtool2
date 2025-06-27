package com.example.painlogger

import android.util.Log
import com.example.painlogger.data.ReminderDao
import com.example.painlogger.data.ReminderEntity
import com.example.painlogger.Reminder
import com.example.painlogger.ReminderCategory
import com.example.painlogger.ReminderMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(
    private val reminderDao: ReminderDao
) {
    companion object {
        private const val TAG = "ReminderRepository"
    }

    // region CRUD Operations
    suspend fun upsertReminder(reminder: Reminder): Long = withContext(Dispatchers.IO) {
        try {
            val entity = ReminderMapper.toEntity(reminder)
            entity?.let {
                reminderDao.insert(it)
            } ?: run {
                Log.e(TAG, "Failed to convert Reminder to Entity: ${reminder.id}")
                throw IllegalArgumentException("Invalid reminder conversion")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error upserting reminder: ${reminder.id}", e)
            throw e
        }
    }

    suspend fun deleteReminder(reminderId: UUID) = withContext(Dispatchers.IO) {
        try {
            reminderDao.deleteReminderById(reminderId)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting reminder: $reminderId", e)
            throw e
        }
    }
    // endregion

    // region Get Operations
    fun getReminderById(reminderId: UUID): Flow<Reminder?> {
        return reminderDao.getReminderByIdFlow(reminderId)
            .map { entity ->
                entity?.let { ReminderMapper.toDomain(it) }
            }
    }

    suspend fun getReminderByIdDirect(reminderId: UUID): Reminder? {
        return reminderDao.getReminderById(reminderId)
            ?.let { ReminderMapper.toDomain(it) }
    }

    fun getAllRemindersStream(): Flow<List<Reminder>> {
        return reminderDao.getAllReminders()
            .map { entities -> convertEntities(entities) }
    }

    fun getEnabledReminders(): Flow<List<Reminder>> {
        return reminderDao.getAllEnabledReminders()
            .map { entities -> convertEntities(entities) }
    }

    fun getRemindersByCategory(category: ReminderCategory): Flow<List<Reminder>> {
        return reminderDao.getRemindersByCategory(category.name)
            .map { entities -> convertEntities(entities) }
    }
    // endregion

    // region Helper Methods
    private fun convertEntities(entities: List<ReminderEntity>): List<Reminder> {
        return entities.mapNotNull { entity ->
            try {
                ReminderMapper.toDomain(entity)
            } catch (e: Exception) {
                Log.e(TAG, "Conversion error for entity: ${entity.id}", e)
                null
            }
        }
    }

    suspend fun getAllRemindersSnapshot(): List<Reminder> {
        return try {
            convertEntities(reminderDao.getAllReminders().first())
        } catch (e: Exception) {
            Log.e(TAG, "Error getting snapshot", e)
            emptyList()
        }
    }
    // endregion
}