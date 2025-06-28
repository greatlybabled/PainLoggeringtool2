package com.example.painloggerwatch.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.UUID

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderRepository: ReminderRepository

    @Inject
    lateinit var workScheduler: WorkScheduler

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED &&
            intent?.action != "android.intent.action.QUICKBOOT_POWERON") {
            return
        }

        Log.d(TAG, "Device boot completed. Rescheduling reminders...")

        coroutineScope.launch {
            try {
                val enabledReminders = reminderRepository.getEnabledReminders().first()
                Log.d(TAG, "Found ${enabledReminders.size} enabled reminders")

                var successCount = 0

                enabledReminders.forEach { reminder ->
                    try {
                        Log.d(TAG, "Scheduling reminder: ${reminder.id}")
                        reminder.config?.let { config ->
                            workScheduler.scheduleReminder(config)
                            successCount++
                        } ?: run {
                            Log.e(TAG, "Failed to schedule reminder ${reminder.id} - ${reminder.title}: config is null")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to schedule reminder ${reminder.id} - ${reminder.title}", e)
                    }
                }

                Log.d(TAG, "Successfully rescheduled $successCount out of ${enabledReminders.size} reminders")
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling reminders after boot", e)
            }
        }

        goAsync()?.let { pendingResult ->
            coroutineScope.launch {
                try {
                    pendingResult.finish()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in async operation", e)
                    pendingResult.finish()
                }
            }
        }
    }
}