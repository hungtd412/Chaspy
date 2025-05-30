package com.example.chaspy;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;

import androidx.multidex.MultiDex;
import androidx.work.Configuration;
import java.util.concurrent.Executors;

import com.example.chaspy.service.ScheduledMessageManager;

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
            // This does not block the UI thread
            
            // Start the scheduled message service with 3-second frequency
            // This method is designed to not block the UI thread
            ScheduledMessageManager.startScheduledMessageWorker(this);
            Log.i(TAG, "Initialized scheduled message scheduler");
            
            // Schedule additional periodic checks for better reliability
            // These use Handler.postDelayed which is non-blocking
            scheduleAdditionalChecks();
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting ScheduledMessageManager: " + e.getMessage(), e);
        }
    }
    
    private void scheduleAdditionalChecks() {
        // Schedule additional checks at specific intervals using non-blocking Handler.postDelayed
        // These provide redundancy in case the second-level checker is killed
        for (int seconds : new int[]{30, 60, 300}) {
            final int delay = seconds;
            handler.postDelayed(() -> {
                ScheduledMessageManager.checkScheduledMessagesNow(this);
                Log.d(TAG, "Running additional scheduled message check at " + delay + " seconds after startup");
            }, seconds * 1000);
        }
    }
    
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .setJobSchedulerJobIdRange(1000, 2000) // Provide a specific range for job IDs
                .setExecutor(Executors.newFixedThreadPool(2)) // Explicitly provide executor
                .build();
    }
    
    @Override
    public void onTerminate() {
        super.onTerminate();
        // Clean up resources
        ScheduledMessageManager.shutdown();
    }
}
