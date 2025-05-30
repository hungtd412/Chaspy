package com.example.chaspy.data.repository;

import android.util.Log;

import com.example.chaspy.data.model.ScheduleMessage;
import com.example.chaspy.data.service.ScheduleMessageFirebaseService;

import java.util.List;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ScheduleMessageRepository {
    private static final String TAG = "ScheduleMsgRepository";
    private final ScheduleMessageFirebaseService firebaseService;

    public ScheduleMessageRepository() {
        firebaseService = new ScheduleMessageFirebaseService();
    }

    public interface ScheduleCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    /**
     * Get all scheduled messages for a specific user
     */
    public void getScheduledMessages(String userId, ScheduleCallback<List<ScheduleMessage>> callback) {
        firebaseService.getScheduledMessages(userId, new ScheduleMessageFirebaseService.ScheduleCallback<List<ScheduleMessage>>() {
            @Override
            public void onSuccess(List<ScheduleMessage> result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Delete a scheduled message from Firebase
     */
    public void deleteScheduledMessage(String messageId, ScheduleCallback<Void> callback) {
        Log.d(TAG, "Deleting scheduled message with ID: " + messageId);
        firebaseService.deleteScheduledMessage(messageId, new ScheduleMessageFirebaseService.ScheduleCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "Successfully deleted scheduled message: " + messageId);
                callback.onSuccess(null);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error deleting scheduled message " + messageId + ": " + error);
                callback.onError(error);
            }
        });
    }

    /**
     * Add a scheduled message
     */
    public void addScheduledMessage(ScheduleMessage message, ScheduleCallback<String> callback) {
        // Generate a new ID if one isn't provided
        String messageId = message.getId();
        if (messageId == null || messageId.isEmpty()) {
            messageId = firebaseService.generateMessageId();
        }

        if (messageId == null) {
            callback.onError("Failed to generate message ID");
            return;
        }

        // Create a final copy of the ID for use in the lambda
        final String finalMessageId = messageId;

        message.setId(finalMessageId);

        // Log the message being added
        Log.d(TAG, "Adding scheduled message: ID=" + finalMessageId +
                ", Sender=" + message.getSenderId() +
                ", Receiver=" + message.getReceiverId() +
                ", Time=" + new Date(message.getSendingTime()).toString() +
                ", ConversationID=" + message.getConversationId());

        firebaseService.addScheduledMessage(message, finalMessageId, new ScheduleMessageFirebaseService.ScheduleCallback<String>() {
            @Override
            public void onSuccess(String result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Get all pending scheduled messages that are due to be sent
     * Enhanced with better logging
     */
    public void getPendingScheduledMessages(long currentTime, ScheduleCallback<List<ScheduleMessage>> callback) {
        String formattedTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(new Date(currentTime));
        Log.d(TAG, "Repository checking for pending messages at " + formattedTime);

        firebaseService.getPendingScheduledMessages(currentTime, new ScheduleMessageFirebaseService.ScheduleCallback<List<ScheduleMessage>>() {
            @Override
            public void onSuccess(List<ScheduleMessage> result) {
                if (result != null && !result.isEmpty()) {
                    Log.d(TAG, "Found " + result.size() + " pending messages at " + formattedTime);

                    // Log details of each message for debugging
                    for (ScheduleMessage message : result) {
                        String scheduledTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                .format(new Date(message.getSendingTime()));

                        Log.d(TAG, String.format("Message #%s to %s scheduled for %s",
                                message.getId().substring(0, Math.min(8, message.getId().length())),
                                message.getReceiverId(),
                                scheduledTime));
                    }
                } else {
                    Log.d(TAG, "No pending messages found at " + formattedTime);
                }
                callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error fetching pending messages: " + error);
                callback.onError(error);
            }
        });
    }

    /**
     * Get all pending scheduled messages that are due to be sent (blocking version)
     * This method blocks until the Firebase operation completes or times out
     * 
     * @param currentTime The current timestamp to compare against
     * @param callback The callback to receive results
     */
    public void getPendingScheduledMessagesBlocking(long currentTime, ScheduleCallback<List<ScheduleMessage>> callback) {
        String formattedTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(new Date(currentTime));
        Log.d(TAG, "Repository checking for pending messages (blocking) at " + formattedTime);

        // Create atomic reference to hold result
        final AtomicReference<List<ScheduleMessage>> resultRef = new AtomicReference<>();
        final AtomicReference<String> errorRef = new AtomicReference<>();
        
        // Use CountDownLatch to block until operation completes
        final CountDownLatch latch = new CountDownLatch(1);
        
        firebaseService.getPendingScheduledMessages(currentTime, new ScheduleMessageFirebaseService.ScheduleCallback<List<ScheduleMessage>>() {
            @Override
            public void onSuccess(List<ScheduleMessage> result) {
                if (result != null && !result.isEmpty()) {
                    Log.d(TAG, "Found " + result.size() + " pending messages at " + formattedTime);
                    
                    // Log details of each message for debugging
                    for (ScheduleMessage message : result) {
                        String scheduledTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                .format(new Date(message.getSendingTime()));
                        
                        Log.d(TAG, String.format("Message #%s to %s scheduled for %s",
                                message.getId().substring(0, Math.min(8, message.getId().length())),
                                message.getReceiverId(),
                                scheduledTime));
                    }
                } else {
                    Log.d(TAG, "No pending messages found at " + formattedTime);
                }
                
                resultRef.set(result);
                latch.countDown();
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error fetching pending messages: " + error);
                errorRef.set(error);
                latch.countDown();
            }
        });
        
        try {
            // Wait up to 5 seconds for the operation to complete
            boolean completed = latch.await(5, TimeUnit.SECONDS);
            
            if (!completed) {
                Log.w(TAG, "Timeout waiting for pending messages");
                callback.onError("Operation timed out");
                return;
            }
            
            if (errorRef.get() != null) {
                callback.onError(errorRef.get());
            } else {
                callback.onSuccess(resultRef.get());
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while waiting for pending messages", e);
            Thread.currentThread().interrupt(); // Restore the interrupted status
            callback.onError("Operation interrupted: " + e.getMessage());
        }
    }

    /**
     * This method has been replaced by deleteScheduledMessage to better reflect its functionality
     * @deprecated Use deleteScheduledMessage instead
     */
    @Deprecated
    public void markScheduledMessageAsSent(String messageId, ScheduleCallback<Void> callback) {
        deleteScheduledMessage(messageId, callback);
    }
}
