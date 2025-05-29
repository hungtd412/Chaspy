package com.example.chaspy.service;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.BackoffPolicy;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.ExistingWorkPolicy;
import androidx.work.Data;

import java.util.concurrent.TimeUnit;
import android.os.Handler;
import android.os.Looper;

public class ScheduledMessageManager {
    private static final String TAG = "ScheduledMessageManager";
    private static final String WORK_NAME = "scheduled_message_worker";
    
    // Check every second using a Handler instead of WorkManager for periodic work
    // WorkManager's minimum interval is 15 minutes, so we use Handler for second-level precision
    private static final long CHECK_INTERVAL_MS = 3000; // 3 second interval
    
    // Handler for second-level checks
    private static Handler secondLevelHandler;
    private static Runnable secondLevelRunnable;
    private static boolean isRunning = false;
    
    // This minimum interval is still used for WorkManager as a backup mechanism
    private static final long REPEAT_INTERVAL = 15;
    private static final TimeUnit REPEAT_INTERVAL_UNIT = TimeUnit.MINUTES;
    
    public static void startScheduledMessageWorker(Context context) {
        try {
            Log.d(TAG, "Setting up scheduled message worker with 1-second interval");
            
            // Set up WorkManager (as backup/redundancy)
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();
            
            // Set up a periodic work request with minimum interval
            PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                    ScheduledMessageWorker.class,
                    REPEAT_INTERVAL, REPEAT_INTERVAL_UNIT)
                    .setConstraints(constraints)
                    .setInitialDelay(5, TimeUnit.SECONDS)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                    .build();
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest);
            
            // Start second-level precision handler
            startSecondLevelChecks(context);
            
            // Also schedule an immediate check
            checkScheduledMessagesNow(context);
            
            Log.d(TAG, "Scheduled message worker has been set up with 1-second interval");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start scheduled message worker: " + e.getMessage(), e);
        }
    }
    
    private static void startSecondLevelChecks(final Context context) {
        // Prevent multiple handlers from running
        if (isRunning) {
            Log.d(TAG, "Second-level checker already running");
            return;
        }
        
        Log.d(TAG, "Starting second-level precision checks every second");
        
        // Create handler on main thread
        if (secondLevelHandler == null) {
            secondLevelHandler = new Handler(Looper.getMainLooper());
        }
        
        // Create runnable that will check messages and reschedule itself
        secondLevelRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    // Schedule a one-time work to check messages
                    OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(ScheduledMessageWorker.class)
                            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                            .build();
                    WorkManager.getInstance(context).enqueue(workRequest);
                    
                    // Reschedule this runnable to run again in 1 second
                    if (isRunning && secondLevelHandler != null) {
                        secondLevelHandler.postDelayed(this, CHECK_INTERVAL_MS);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in second-level check: " + e.getMessage());
                    // Try to recover by scheduling again
                    if (isRunning && secondLevelHandler != null) {
                        secondLevelHandler.postDelayed(this, CHECK_INTERVAL_MS);
                    }
                }
            }
        };
        
        // Start the recurring checks
        isRunning = true;
        secondLevelHandler.post(secondLevelRunnable);
    }
    
    public static void stopScheduledMessageWorker(Context context) {
        try {
            Log.d(TAG, "Stopping scheduled message worker");
            
            // Stop WorkManager tasks
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
            
            // Stop second-level handler
            stopSecondLevelChecks();
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop scheduled message worker: " + e.getMessage(), e);
        }
    }
    
    private static void stopSecondLevelChecks() {
        if (secondLevelHandler != null && secondLevelRunnable != null) {
            Log.d(TAG, "Stopping second-level precision checks");
            secondLevelHandler.removeCallbacks(secondLevelRunnable);
            isRunning = false;
        }
    }
    
    // Additional method for one-time immediate check with high priority
    public static void checkScheduledMessagesNow(Context context) {
        try {
            Log.d(TAG, "Triggering immediate scheduled message check");
            OneTimeWorkRequest immediateWorkRequest = new OneTimeWorkRequest.Builder(
                    ScheduledMessageWorker.class)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build();
            WorkManager.getInstance(context).enqueue(immediateWorkRequest);
        } catch (Exception e) {
            Log.e(TAG, "Failed to trigger immediate check: " + e.getMessage(), e);
        }
    }
}
