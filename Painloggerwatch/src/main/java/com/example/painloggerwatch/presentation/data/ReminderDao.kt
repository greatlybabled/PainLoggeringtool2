// File: ReminderDao.kt
package com.example.painloggerwatch.presentation.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface ReminderDao {
    // Insert operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reminders: List<ReminderEntity>)

    // Update operations
    @Update
    suspend fun update(reminder: ReminderEntity)

    @Query("UPDATE reminders SET isEnabled = :isEnabled WHERE id = :reminderId")
    suspend fun updateReminderEnabled(reminderId: UUID, isEnabled: Boolean)

    // Delete operations
    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :reminderId")
    suspend fun deleteReminderById(reminderId: UUID)

    // Query operations
    @Query("SELECT * FROM reminders")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isEnabled = 1")
    fun getAllEnabledReminders(): Flow<List<ReminderEntity>>

    // Get by ID with different return types
    @Query("SELECT * FROM reminders WHERE id = :reminderId")
    fun getReminderByIdFlow(reminderId: UUID): Flow<ReminderEntity?>

    @Query("SELECT * FROM reminders WHERE id = :reminderId")
    suspend fun getReminderById(reminderId: UUID): ReminderEntity?

    // Filtered queries
    @Query("SELECT * FROM reminders WHERE category = :categoryName")
    fun getRemindersByCategory(categoryName: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE type = :reminderType")
    fun getRemindersByType(reminderType: ReminderType): Flow<List<ReminderEntity>>

    // Query for specific day
    @Query("SELECT * FROM reminders WHERE json_array_length(activeDays) > 0")
    fun getRemindersWithActiveDays(): Flow<List<ReminderEntity>>

    // Synchronous version for workers/background operations
    @Query("SELECT * FROM reminders WHERE id = :reminderId")
    fun getReminderByIdSync(reminderId: UUID): ReminderEntity?
}