// File: app/src/main/java/com/example/painlogger/data/ReminderDatabase.java

package com.example.painlogger.data;

import android.content.Context;
// Import ReminderEntity IF it's defined in a different package.
// If ReminderEntity.kt is in the *exact same package* (com.example.painlogger.data),
// this import might be redundant, but keeping it won't hurt and can sometimes
// help the IDE.
import com.example.painlogger.data.ReminderEntity;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters; // Make sure you have this import

import com.example.painlogger.ReminderConverters; // Import your TypeConverter class (it's in com.example.painlogger package)

// Define the database. List all entities belonging to this database here.
// Increment the version number whenever you change the database schema.
@Database(entities = {ReminderEntity.class}, version = 1, exportSchema = false)
@TypeConverters({ReminderConverters.class}) // Reference your TypeConverter class
public abstract class ReminderDatabase extends RoomDatabase {

    // Abstract method to get your DAO
    public abstract ReminderDao reminderDao();

    // Singleton instance
    private static volatile ReminderDatabase INSTANCE;

    // Method to get the database instance
    public static ReminderDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (ReminderDatabase.class) {
                if (INSTANCE == null) {
                    // Create database here
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    ReminderDatabase.class, "reminder_database") // "reminder_database" is the database name
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    // Callback is fine if you need it
}