package com.example.painlogger;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;
import java.util.UUID;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.Data;

import com.example.painlogger.Reminder;
import com.example.painlogger.ReminderCategory;
import com.example.painlogger.ReminderType;
import com.example.painlogger.data.ReminderDatabase;
import com.example.painlogger.data.ReminderDao;
import com.example.painlogger.data.ReminderEntity;
import com.example.painlogger.ReminderMapper;

public class ReminderWorker extends Worker {
    private static final String TAG = "ReminderWorker";


    public static final String CHANNEL_ID = "pain_logger_reminder_channel";
    public static final String REMINDER_ID_KEY = "reminder_id";

    private ReminderDao reminderDao;

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        Log.d("ReminderWorker", "Initializing ReminderWorker");
        try {
            ReminderDatabase database = ReminderDatabase.getDatabase(context.getApplicationContext());
            reminderDao = database.reminderDao();
            Log.d("ReminderWorker", "ReminderDao initialized successfully");
        } catch (Exception e) {
            Log.e("ReminderWorker", "Failed to get database instance during initialization", e);
            reminderDao = null;
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("ReminderWorker", "doWork() started at " + new java.util.Date() + ". Worker ID: " + getId());

        if (reminderDao == null) {
            Log.e("ReminderWorker", "CRITICAL: ReminderDao is not initialized. Cannot fetch reminder.");
            return Result.retry();
        }

        Log.d("ReminderWorker", "Creating notification channel...");
        createNotificationChannel();

        Data inputData = getInputData();
        Log.d("ReminderWorker", "Input data keys: " + inputData.getKeyValueMap().keySet());

        String reminderIdString = inputData.getString(REMINDER_ID_KEY);
        Log.d("ReminderWorker", "Processing reminder ID from input data: " + reminderIdString);

        if (reminderIdString == null || reminderIdString.isEmpty()) {
            Log.e("ReminderWorker", "ERROR: Reminder ID is missing from input data. Input data: " + inputData);
            return Result.failure();
        }

        UUID reminderId;
        try {
            Log.d("ReminderWorker", "Parsing reminder ID string to UUID...");
            reminderId = UUID.fromString(reminderIdString);
            Log.d("ReminderWorker", "Successfully parsed reminder ID: " + reminderId);
        } catch (IllegalArgumentException e) {
            Log.e("ReminderWorker", "ERROR: Invalid Reminder ID format: " + reminderIdString, e);
            return Result.failure();
        }

        Log.d("ReminderWorker", "Attempting to fetch reminder from database with ID: " + reminderId);
        ReminderEntity reminderEntity = null;
        try {
            reminderEntity = reminderDao.getReminderByIdSync(reminderId);
            Log.d("ReminderWorker", "Database query completed for reminder ID: " + reminderId);
        } catch (Exception e) {
            Log.e("ReminderWorker", "Exception while querying database for reminder ID: " + reminderId, e);
            return Result.failure();
        }

        Reminder reminder;
        if (reminderEntity == null) {
            Log.w("ReminderWorker", "Reminder not found in database for ID: " + reminderId + ". " +
                    "Using input data to create a temporary reminder.");
            
            // Create a temporary reminder from the input data
            String title = inputData.getString("title");
            String message = inputData.getString("message");
            String categoryStr = inputData.getString("category");
            
            if (title == null || message == null) {
                Log.e("ReminderWorker", "Missing title or message in input data");
                return Result.failure();
            }
            
            // Determine the category from input data
            ReminderCategory category;
            if (categoryStr != null) {
                try {
                    category = ReminderCategory.valueOf(categoryStr);
                    Log.d("ReminderWorker", "Using category from input data: " + category);
                } catch (IllegalArgumentException e) {
                    Log.w("ReminderWorker", "Invalid category in input data: " + categoryStr + ". Using GENERAL as default.");
                    category = ReminderCategory.GENERAL;
                }
            } else {
                // Try to determine category from title
                if (title.toUpperCase().contains("DETAILED")) {
                    category = ReminderCategory.DETAILED;
                    Log.d("ReminderWorker", "Determined DETAILED category from title");
                } else {
                    category = ReminderCategory.GENERAL;
                    Log.d("ReminderWorker", "Using default GENERAL category");
                }
            }
            
            // Create a basic reminder with the input data
            reminder = new Reminder(
                reminderId,
                title,
                category,
                ReminderType.INTERVAL,    // Default type
                true,                    // Enabled
                java.util.Collections.singleton(java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)), // Today
                null                     // No config
            );
        } else {
            // Convert the entity to a domain model
            reminder = ReminderMapper.toDomain(reminderEntity);
            if (reminder == null) {
                Log.e("ReminderWorker", "Failed to convert entity to domain model for ID: " + reminderId);
                return Result.failure();
            }
        }

        Log.d(TAG, "Reminder details: " +
                "ID=" + reminder.getId() + ", " +
                "Title=" + reminder.getTitle() + ", " +
                "Type=" + reminder.getType() + ", " +
                "Enabled=" + reminder.isEnabled());
        // Reminder has already been created or converted above

        Log.d("ReminderWorker", "Fetched and converted Reminder: " + reminder.toString());

        if (!reminder.isEnabled()) {
            Log.d("ReminderWorker", "Reminder " + reminderId + " is disabled. Skipping notification.");
            return Result.success();
        }

        Calendar calendar = Calendar.getInstance();
        int today = calendar.get(Calendar.DAY_OF_WEEK);
        String[] dayNames = {"SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"};
        String todayName = dayNames[calendar.get(Calendar.DAY_OF_WEEK) - 1];

        Set<Integer> activeDays = reminder.getActiveDays();
        Log.d("ReminderWorker", "Active days for reminder: " + (activeDays != null ? activeDays.toString() : "null"));
        boolean isActiveToday = activeDays != null && !activeDays.isEmpty() && activeDays.contains(today);

        Log.d("ReminderWorker", "Checking active days for reminder " + reminderId + ":\n" +
                "- Today is: " + todayName + " (" + today + ")\n" +
                "- Active days: " + (activeDays != null ? activeDays.toString() : "null") + "\n" +
                "- Is active today: " + isActiveToday);

        if (!isActiveToday) {
            Log.d("ReminderWorker", "Reminder " + reminderId + " is not active today (" + todayName + "). Active days: " +
                    (activeDays != null ? activeDays.stream().map(d -> dayNames[d-1]).collect(java.util.stream.Collectors.toList()) : "none") +
                    ". Skipping notification.");
            return Result.success();
        }

        String notificationTitle;
        String notificationText;

        Log.d("ReminderWorker", "Determining notification content for category: " + reminder.getCategory());
        switch (reminder.getCategory()) {
            case DETAILED:
                notificationTitle = "Detailed Pain Log Reminder";
                notificationText = "Time to do a detailed pain assessment";
                break;
            case GENERAL:
                notificationTitle = "General Pain Log Reminder";
                notificationText = "Time to do a quick pain assessment";
                break;
            default:
                notificationTitle = "Pain Logger Reminder";
                notificationText = "Time to log your pain";
                break;
        }
        Log.d("ReminderWorker", "Notification title: " + notificationTitle + ", text: " + notificationText);

        // Create intent that starts logging immediately
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("from_notification", true);
        intent.putExtra("start_logging_immediately", true);
        
        // Add extra to indicate which type of logging to start
        if (reminder.getCategory() == ReminderCategory.DETAILED) {
            intent.putExtra("open_fragment", "detailed");
            Log.d("ReminderWorker", "Setting intent to open detailed logging flow immediately");
        } else {
            intent.putExtra("open_fragment", "general");
            Log.d("ReminderWorker", "Setting intent to open general logging flow immediately");
        }

        int pendingIntentRequestCode = reminderId.hashCode();
        Log.d("ReminderWorker", "Creating PendingIntent with request code: " + pendingIntentRequestCode);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                pendingIntentRequestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        int notificationId = reminderId.hashCode();
        Log.d("ReminderWorker", "Building notification with ID: " + notificationId);

        // Create a "Dismiss" action
        Intent dismissIntent = new Intent(getApplicationContext(), MainActivity.class);
        dismissIntent.setAction("com.example.painlogger.DISMISS_NOTIFICATION");
        dismissIntent.putExtra("notification_id", notificationId);
        
        PendingIntent dismissPendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                pendingIntentRequestCode + 1,
                dismissIntent,
                PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(notificationTitle)
                .setContentText(notificationText + " - Tap to log now")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(notificationText + " - Tap to log now"))
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(R.drawable.ic_launcher_foreground, "Dismiss", dismissPendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        try {
            Log.d("ReminderWorker", "Attempting to show notification...");
            notificationManager.notify(notificationId, builder.build());
            Log.d("ReminderWorker", "Notification shown for Reminder ID: " + reminderId + " with Notification ID: " + notificationId);
        } catch (SecurityException e) {
            Log.e("ReminderWorker", "Notification permission denied. Cannot show notification for Reminder ID: " + reminderId, e);
            return Result.success();
        } catch (Exception e) {
            Log.e("ReminderWorker", "Failed to show notification for Reminder ID: " + reminderId, e);
            return Result.failure();
        }

        if (reminder.getType() == ReminderType.SPECIFIC_TIME) {
            try {
                Log.d("ReminderWorker", "Attempting to reschedule SPECIFIC_TIME reminder for ID: " + reminderId);
                WorkScheduler workScheduler = new WorkScheduler(getApplicationContext());
                workScheduler.rescheduleReminder(reminder.getConfig());
                Log.d("ReminderWorker", "Successfully rescheduled next SPECIFIC_TIME reminder for ID: " + reminderId);
            } catch (Exception e) {
                Log.e("ReminderWorker", "ERROR: Failed to reschedule SPECIFIC_TIME reminder for ID: " + reminderId, e);
                return Result.retry();
            }
        }

        Log.d("ReminderWorker", "doWork() finished successfully for Reminder ID: " + reminderId);
        return Result.success();
    }

    private void createNotificationChannel() {
        Log.d("ReminderWorker", "Checking if notification channel needs to be created...");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Use string resources for channel name and description
            CharSequence name = "Pain Logger Reminders";
            String description = "Notifications for pain logging reminders";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            
            // Configure additional channel properties
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setShowBadge(true);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            NotificationManager notificationManager = getApplicationContext().getSystemService(NotificationManager.class);

            if (notificationManager != null) {
                Log.d("ReminderWorker", "Creating notification channel: " + CHANNEL_ID);
                notificationManager.createNotificationChannel(channel);
                
                // Verify channel was created
                NotificationChannel createdChannel = notificationManager.getNotificationChannel(CHANNEL_ID);
                if (createdChannel != null) {
                    Log.d("ReminderWorker", "Notification channel successfully created: " + CHANNEL_ID);
                } else {
                    Log.e("ReminderWorker", "Failed to create notification channel: " + CHANNEL_ID);
                }
            } else {
                Log.e("ReminderWorker", "NotificationManager is null. Cannot create notification channel.");
            }
        } else {
            Log.d("ReminderWorker", "Notification channels not required on API levels below 26.");
        }
    }
}