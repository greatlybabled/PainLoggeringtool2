
package com.example.painloggerwatch.presentation

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import com.example.painlogger.data.model.ReminderConfig
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class PainLoggerApplication : Application() {

    @Inject
    lateinit var workScheduler: WorkScheduler
    
    @Inject
    lateinit var reminderRepository: ReminderRepository
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onCreate() {
        super.onCreate()
        
        // Create and save a default reminder
        applicationScope.launch {
            try {
                // Create a reminder with UUID
                val reminderId = UUID.randomUUID()
                
                // Create the reminder config
                val periodicConfig = ReminderConfig.IntervalConfig(
                    id = reminderId.toString(),
                    title = "Daily Pain Logger Reminder",
                    message = "It's time to log your pain level",
                    intervalHours = 24,  // For daily reminder
                    intervalMinutes = 0
                )
                
                // Create the full reminder object
                val reminder = Reminder(
                    id = reminderId,
                    title = "Daily Pain Logger",
                    category = ReminderCategory.GENERAL,
                    type = ReminderType.INTERVAL,
                    isEnabled = true,
                    activeDays = setOf(1, 2, 3, 4, 5, 6, 7), // All days of the week
                    config = periodicConfig
                )
                
                // Save to database first
                reminderRepository.upsertReminder(reminder)
                Log.d("PainLoggerApp", "Default reminder saved to database with ID: $reminderId")
                
                // Then schedule it with the specific category
                workScheduler.scheduleReminder(periodicConfig, ReminderCategory.GENERAL)
                Log.d("PainLoggerApp", "Default GENERAL reminder scheduled with WorkManager")
            } catch (e: Exception) {
                Log.e("PainLoggerApp", "Error creating default reminder", e)
            }
        }
    }
}