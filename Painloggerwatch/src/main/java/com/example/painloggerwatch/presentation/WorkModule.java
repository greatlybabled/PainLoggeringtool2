package com.example.painloggerwatch.presentation; // Or your module's package

import android.content.Context;
import com.example.painlogger.WorkScheduler;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class) // Or your desired Hilt component
public class WorkModule {

    @Provides
    @Singleton // WorkScheduler should likely be a singleton
    public WorkScheduler provideWorkScheduler(@ApplicationContext Context context) {
        return new WorkScheduler(context); // Pass the provided context
    }
}