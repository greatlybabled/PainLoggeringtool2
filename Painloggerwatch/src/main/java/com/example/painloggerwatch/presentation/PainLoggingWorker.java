package com.example.painloggerwatch.presentation;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class PainLoggingWorker extends Worker {
    private static final String TAG = "PainLoggingWorker";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "pain_logger_channel";
    private static final String CHANNEL_NAME = "Pain Logger Notifications";

    public PainLoggingWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        createNotificationChannel();
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            // 1. Check notification permission for Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        getApplicationContext(),
                        Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Notification permission not granted");
                    return Result.success(); // Graceful degradation
                }
            }

            // 2. Create notification with proper PendingIntent flags
            Notification notification = buildNotification();
            NotificationManagerCompat.from(getApplicationContext())
                    .notify(NOTIFICATION_ID, notification);

            // 3. Maintain foreground service requirements
            setForegroundAsync(new ForegroundInfo(
                    NOTIFICATION_ID + 1,
                    buildForegroundNotification()
            ));

            return Result.success();
        } catch (SecurityException e) {
            Log.e(TAG, "Security Exception: " + e.getMessage());
            return Result.retry(); // Or failure based on requirements
        } catch (Exception e) {
            Log.e(TAG, "Work failed: " + e.getMessage());
            return Result.failure();
        }
    }

    private Notification buildNotification() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.putExtra("from_notification", true);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        return new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle("Pain Logging Reminder")
                .setContentText("Tap to log your current pain status")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(PendingIntent.getActivity(
                        getApplicationContext(),
                        0,
                        intent,
                        flags
                ))
                .setAutoCancel(true)
                .build();
    }

    private Notification buildForegroundNotification() {
        return new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle("Pain Logger Active")
                .setContentText("Monitoring pain levels")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Channel for pain logging notifications");

            NotificationManager manager = getApplicationContext()
                    .getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}