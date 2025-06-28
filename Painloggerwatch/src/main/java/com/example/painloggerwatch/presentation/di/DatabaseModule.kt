package com.example.painloggerwatch.presentation.di

import android.content.Context
import androidx.room.Room
import com.example.painlogger.AppDatabase // Import your AppDatabase class
import com.example.painlogger.data.ReminderDao // Import your ReminderDao interface
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Hilt Module for providing database-related dependencies
@Module
@InstallIn(SingletonComponent::class) // Install this module in the application-level component
object DatabaseModule {

    // Provides a singleton instance of the AppDatabase
    @Provides
    @Singleton // Indicates that only one instance of the database will be created
    fun provideDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "app_database" // Match the database name used in AppDatabase
        )
            // Wipes and rebuilds instead of migrating if no Migration object (Development only)
            .fallbackToDestructiveMigration()
            .build()
    }

    // Provides the ReminderDao by getting the database instance
    @Provides
    fun provideReminderDao(db: AppDatabase): ReminderDao {
        return db.reminderDao()
    }
}