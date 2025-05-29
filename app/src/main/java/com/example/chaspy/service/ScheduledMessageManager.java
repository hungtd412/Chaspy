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

public class ScheduledMessageManager {
    private static final String TAG = "ScheduledMessageManager";
    private static final String WORK_NAME = "scheduled_message_worker";
    
    // Run every minute for more frequent checking
    private static final long REPEAT_INTERVAL = 1;
    private static final TimeUnit REPEAT_INTERVAL_UNIT = TimeUnit.MINUTES;
    
    // Minimum flex interval allowed by WorkManager
    private static final long FLEX_INTERVAL = 0; // Set to 0 for as exact timing as possible
    private static final TimeUnit FLEX_INTERVAL_UNIT = TimeUnit.MINUTES;
    
    public static void startScheduledMessageWorker(Context context) {
        try {
            Log.d(TAG, "Setting up scheduled message worker with 1-minute interval");
            
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();
            
            // Set up a periodic work request with a 1-minute interval
            PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                    ScheduledMessageWorker.class,
                    REPEAT_INTERVAL, REPEAT_INTERVAL_UNIT)
                    .setConstraints(constraints)
                    .setInitialDelay(5, TimeUnit.SECONDS) // Small initial delay
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS) // Fast retry
                    .build();
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest);
            
            // Also schedule an immediate check
            checkScheduledMessagesNow(context);
            
            // Schedule additional checks for better reliability
            scheduleAdditionalChecks(context);
            
            Log.d(TAG, "Scheduled message worker has been set up with 1-minute interval");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start scheduled message worker: " + e.getMessage(), e);
        }
    }
    
    // Schedule additional checks at specific intervals for better reliability
    private static void scheduleAdditionalChecks(Context context) {
        // Schedule checks at 15-second, 30-second, and 45-second marks
        for (int seconds : new int[]{15, 30, 45}) {
            String workName = "additional_check_" + seconds;
            
            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(ScheduledMessageWorker.class)
                    .setInitialDelay(seconds, TimeUnit.SECONDS)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build();
            
            WorkManager.getInstance(context).enqueueUniqueWork(
                    workName,
                    ExistingWorkPolicy.REPLACE,
                    workRequest);
            
            Log.d(TAG, "Scheduled additional check at " + seconds + " seconds");
        }
    }
    
    public static void stopScheduledMessageWorker(Context context) {
        try {
            Log.d(TAG, "Stopping scheduled message worker");
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop scheduled message worker: " + e.getMessage(), e);
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
