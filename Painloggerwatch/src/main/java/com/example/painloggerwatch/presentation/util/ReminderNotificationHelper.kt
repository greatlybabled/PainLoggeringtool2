
package com.example.painloggerwatch.presentation.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.painlogger.MainActivity
import com.example.painlogger.R
import com.example.painlogger.Reminder
import com.example.painlogger.ReminderType

private const val TAG = "ReminderNotificationHelper"
private const val CHANNEL_ID_DETAILED = "detailed_logging_channel"
private const val CHANNEL_ID_GENERAL = "general_logging_channel"

object ReminderNotificationHelper {

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "Creating notification channels")
            try {
                val detailedChannel = NotificationChannel(
                    CHANNEL_ID_DETAILED,
                    "Detailed Logging Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminders for detailed pain logging"
                    enableLights(true)
                    lightColor = android.graphics.Color.RED
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                }

                val generalChannel = NotificationChannel(
                    CHANNEL_ID_GENERAL,
                    "General Logging Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Reminders for general pain logging"
                    enableLights(true)
                    lightColor = android.graphics.Color.RED
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                }

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(detailedChannel)
                notificationManager.createNotificationChannel(generalChannel)
                Log.d(TAG, "Successfully created notification channels")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create notification channels", e)
            }
        } else {
            Log.d(TAG, "Skipping notification channel creation (requires API 26+)")
        }
    }

    fun showReminderNotification(context: Context, reminder: Reminder) {
        Log.d(TAG, "showReminderNotification called for reminder: ${reminder.id} (${reminder.title}})")
        
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Log notification manager status
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channelId = if (reminder.type == ReminderType.DETAILED) CHANNEL_ID_DETAILED else CHANNEL_ID_GENERAL
                val channel = notificationManager.getNotificationChannel(channelId)
                Log.d(TAG, "Notification channel status: ${if (channel != null) "Exists" else "Does not exist"}")
                if (channel != null) {
                    Log.d(TAG, "Channel importance: ${channel.importance}")
                }
            }

            // Create an intent for the notification tap action
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("open_fragment", if (reminder.type == ReminderType.DETAILED) "detailed" else "general")
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                reminder.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            Log.d(TAG, "Created pending intent with request code: ${reminder.id.hashCode()}")

            // Build the notification
            val isDetailed = reminder.type == ReminderType.DETAILED
            val channelId = if (isDetailed) CHANNEL_ID_DETAILED else CHANNEL_ID_GENERAL
            val notificationTitle = when (reminder.type) {
                ReminderType.DETAILED -> "Detailed Logging Reminder"
                ReminderType.SPECIFIC_TIME -> "Specific Time Reminder"
                ReminderType.INTERVAL -> "Interval Reminder"
            }
            
            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(notificationTitle)
                .setContentText("Tap to log your pain (${reminder.type})")
                .setPriority(if (isDetailed) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(longArrayOf(0, 1000, 500, 1000))
                .setLights(Color.RED, 3000, 3000)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)

            Log.d(TAG, "Notification built with title: $notificationTitle")

            // Add action buttons based on reminder type
            val actionText = when (reminder.type) {
                ReminderType.SPECIFIC_TIME -> "Log Pain"
                ReminderType.INTERVAL -> "Snooze"
                ReminderType.DETAILED -> "Log Details"
            }
            
            val actionIntent = PendingIntent.getActivity(
                context,
                reminder.id.hashCode(),
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("reminder_id", reminder.id.toString())
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            builder.addAction(
                R.drawable.ic_launcher_foreground,
                actionText,
                actionIntent
            )
            Log.d(TAG, "Added $actionText action for ${reminder.type} reminder")

            // Show the notification
            val notificationId = Math.abs(reminder.id.hashCode())
            Log.d(TAG, "Displaying notification with ID: $notificationId")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Check notification permission for Android 13+
                val notificationManagerCompat = NotificationManagerCompat.from(context)
                if (notificationManagerCompat.areNotificationsEnabled()) {
                    notificationManager.notify(notificationId, builder.build())
                    Log.d(TAG, "Notification displayed successfully")
                } else {
                    Log.e(TAG, "Notification permission not granted")
                }
            } else {
                notificationManager.notify(notificationId, builder.build())
                Log.d(TAG, "Notification displayed successfully")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification for reminder: ${reminder.id}", e)
        }
    }
}
