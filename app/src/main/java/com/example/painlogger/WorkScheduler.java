package com.example.painlogger;

import android.content.Context;
import android.util.Log;
import androidx.work.*;
import com.example.painlogger.data.model.ReminderConfig;
import com.example.painlogger.data.model.ReminderConfig.IntervalConfig;
import com.example.painlogger.data.model.ReminderConfig.SpecificTimeConfig;
import com.example.painlogger.data.model.ReminderConfig.Time;
import com.example.painlogger.ReminderWorker;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

/**
 * WorkScheduler schedules and cancels reminders using WorkManager.
 *
 * IMPORTANT: For SPECIFIC_TIME reminders, the ReminderWorker must call
 * WorkScheduler.scheduleReminder(reminder) again after firing to ensure
 * the next occurrence is scheduled. This is because OneTimeWorkRequest
 * only fires once.
 */
public class WorkScheduler {
    private static final String TAG = "WorkScheduler";
    private final Context context;
    private final WorkManager workManager;

    @Inject
    public WorkScheduler(Context context) {
        this.context = context;
        this.workManager = WorkManager.getInstance(context);
    }

    public void scheduleReminder(ReminderConfig reminderConfig) {
        scheduleReminder(reminderConfig, null);
    }
    
    public void scheduleReminder(ReminderConfig reminderConfig, ReminderCategory category) {
        WorkRequest workRequest;

        // If a specific category is provided, update the title to include it
        if (category != null) {
            String prefix = category == ReminderCategory.DETAILED ? "DETAILED" : "GENERAL";
            if (reminderConfig instanceof IntervalConfig) {
                IntervalConfig config = (IntervalConfig) reminderConfig;
                if (!config.getTitle().toUpperCase().contains(prefix)) {
                    config = new IntervalConfig(
                        config.getId(),
                        prefix + " " + config.getTitle(),
                        config.getMessage(),
                        config.getIntervalHours(),
                        config.getIntervalMinutes()
                    );
                    reminderConfig = config;
                }
            } else if (reminderConfig instanceof SpecificTimeConfig) {
                SpecificTimeConfig config = (SpecificTimeConfig) reminderConfig;
                if (!config.getTitle().toUpperCase().contains(prefix)) {
                    config = new SpecificTimeConfig(
                        config.getId(),
                        prefix + " " + config.getTitle(),
                        config.getMessage(),
                        config.getTimes()
                    );
                    reminderConfig = config;
                }
            }
        }

        if (reminderConfig instanceof IntervalConfig) {
            workRequest = createPeriodicRequest((IntervalConfig) reminderConfig);
        } else if (reminderConfig instanceof SpecificTimeConfig) {
            workRequest = createOneTimeRequest((SpecificTimeConfig) reminderConfig);
        } else {
            Log.e(TAG, "Unknown reminder config type");
            return;
        }

        workManager.enqueue(workRequest);
        Log.i(TAG, "Scheduled reminder with ID: " + workRequest.getId());
    }

    public void cancelReminder(String reminderId) {
        workManager.cancelAllWorkByTag(reminderId);
        Log.i(TAG, "Cancelled reminder with ID: " + reminderId);
    }

    private PeriodicWorkRequest createPeriodicRequest(IntervalConfig config) {
        // Determine category from title or use default
        String category = determineCategory(config.getTitle());
        
        Data inputData = new Data.Builder()
                .putString("reminder_id", config.getId())
                .putString("title", config.getTitle())
                .putString("message", config.getMessage())
                .putString("category", category)
                .build();

        Log.d(TAG, "Creating periodic request with category: " + category + " for reminder: " + config.getId());

        return new PeriodicWorkRequest.Builder(
                ReminderWorker.class,
                config.getIntervalHours(),
                TimeUnit.HOURS)
                .setInputData(inputData)
                .addTag(config.getId())
                .build();
    }
    
    private String determineCategory(String title) {
        if (title != null && title.toUpperCase().contains("DETAILED")) {
            return ReminderCategory.DETAILED.name();
        } else {
            return ReminderCategory.GENERAL.name();
        }
    }

    private OneTimeWorkRequest createOneTimeRequest(SpecificTimeConfig config) {
        // Determine category from title or use default
        String category = determineCategory(config.getTitle());
        
        Data inputData = new Data.Builder()
                .putString("reminder_id", config.getId())
                .putString("title", config.getTitle())
                .putString("message", config.getMessage())
                .putString("category", category)
                .build();
                
        Log.d(TAG, "Creating one-time request with category: " + category + " for reminder: " + config.getId());

        Calendar calendar = Calendar.getInstance();
        // Get the first time from the list of times
        Time reminderTime = config.getTimes().get(0);

        // Set reminder time
        calendar.set(Calendar.HOUR_OF_DAY, reminderTime.getHour());
        calendar.set(Calendar.MINUTE, reminderTime.getMinute());
        calendar.set(Calendar.SECOND, 0);

        // If the time has already passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        long delayMillis = calendar.getTimeInMillis() - System.currentTimeMillis();

        return new OneTimeWorkRequest.Builder(ReminderWorker.class)
                .setInputData(inputData)
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .addTag(config.getId())
                .build();
    }

    public void cancelAllReminders() {
        workManager.cancelAllWork();
        Log.i(TAG, "Cancelled all reminders");
    }

    public void rescheduleReminder(ReminderConfig config) {
        cancelReminder(config.getId());
        scheduleReminder(config);
        Log.i(TAG, "Rescheduled reminder with ID: " + config.getId());
    }
}