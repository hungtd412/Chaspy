package com.example.chaspy;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;

import androidx.multidex.MultiDex;
import androidx.work.Configuration;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.chaspy.service.ScheduledMessageManager;
import com.example.chaspy.service.ScheduledMessageWorker;

import java.util.concurrent.TimeUnit;

public class ChaspyApplication extends Application implements Configuration.Provider {
    private static final String TAG = "ChaspyApplication";
    private Handler handler = new Handler(Looper.getMainLooper());
    
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        // Initialize MultiDex before anything else
        try {
            MultiDex.install(this);
        } catch (Exception e) {
            Log.e(TAG, "MultiDex installation failed: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            // Initialize WorkManager with our custom configuration
            WorkManager.initialize(
                this,
                getWorkManagerConfiguration()
            );
            
            // Start the scheduled message service with 1-minute frequency
            ScheduledMessageManager.startScheduledMessageWorker(this);
            Log.i(TAG, "Initialized minute-by-minute message scheduler");
            
            // Schedule additional periodic checks for better reliability
            scheduleAdditionalChecks();
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting ScheduledMessageManager: " + e.getMessage(), e);
        }
    }
    
    private void scheduleAdditionalChecks() {
        // Schedule additional checks at specific intervals
        // This helps ensure we don't miss any messages
        for (int minutes = 1; minutes <= 5; minutes++) {
            final int delay = minutes;
            handler.postDelayed(() -> {
                ScheduledMessageManager.checkScheduledMessagesNow(this);
                Log.d(TAG, "Running additional scheduled message check at " + delay + " minutes after startup");
            }, minutes * 60 * 1000);
        }
    }
    
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .setJobSchedulerJobIdRange(1000, 2000) // Provide a specific range for job IDs
                .build();
    }
}
