package com.example.painlogger

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.Room // Import Room builder
import android.content.Context // Import Context
import com.example.painlogger.data.ReminderDao
import com.example.painlogger.data.ReminderEntity // Import the ReminderEntity

// Define the database with the entities it contains and the version number.
// Export schema is recommended for version control, but can be set to false initially for simplicity.
@Database(entities = [ReminderEntity::class], version = 2, exportSchema = false)
// Tell Room to use the TypeConverters we created for handling complex types
@TypeConverters(ReminderConverters::class)
abstract class AppDatabase : RoomDatabase() {

    // Define the abstract methods for your DAOs
    abstract fun reminderDao(): ReminderDao

    companion object {
        // Singleton prevents multiple instances of database opening at the same time.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database" // Database name
                )
                    // Wipes and rebuilds instead of migrating if no Migration object.
                    // Migration is recommended in production app, but for development, this is simpler.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}