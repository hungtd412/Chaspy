package com.example.chaspy.data.service;

import androidx.annotation.NonNull;

import com.example.chaspy.data.model.ScheduleMessage;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.logging.Logger;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;
import android.util.Log;

public class ScheduleMessageFirebaseService {
    private final DatabaseReference scheduleMessagesRef;
    private static final String TAG = "SchedMsgFirebaseService";

    public ScheduleMessageFirebaseService() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        scheduleMessagesRef = database.getReference("schedule_messages");
    }

    public interface ScheduleCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    /**
     * Get all scheduled messages for a specific user
     */
    public void getScheduledMessages(String userId, ScheduleCallback<List<ScheduleMessage>> callback) {
        scheduleMessagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<ScheduleMessage> messages = new ArrayList<>();

                for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                    String messageId = messageSnapshot.getKey();
                    String senderId = messageSnapshot.child("sender_id").getValue(String.class);

                    // Only add messages where the user is the sender
                    if (senderId != null && senderId.equals(userId)) {
                        String receiverId = messageSnapshot.child("receiver_id").getValue(String.class);
                        String content = messageSnapshot.child("message_content").getValue(String.class);
                        String timeStr = messageSnapshot.child("sending_time").getValue(String.class);

                        if (receiverId != null && content != null && timeStr != null) {
                            try {
                                // Parse string timestamp to long
                                long time = Long.parseLong(timeStr);
                                ScheduleMessage scheduleMessage = new ScheduleMessage(
                                        messageId, senderId, receiverId, content, time);
                                messages.add(scheduleMessage);
                            } catch (NumberFormatException e) {
                                // Skip invalid timestamp entries
                                continue;
                            }
                        }
                    }
                }

                callback.onSuccess(messages);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                callback.onError("Failed to load scheduled messages: " + databaseError.getMessage());
            }
        });
    }

    /**
     * Get pending messages that need to be sent (sending_time <= currentTime)
     * Enhanced with better logging and time comparison
     */
    public void getPendingScheduledMessages(long currentTime, ScheduleCallback<List<ScheduleMessage>> callback) {
        // Format current time for logging
        String formattedCurrentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                .format(new Date(currentTime));
        
        Log.d(TAG, "Checking for pending messages at current time: " + formattedCurrentTime);
        
        scheduleMessagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<ScheduleMessage> pendingMessages = new ArrayList<>();
                int totalMessages = 0;
                int skippedFutureMessages = 0;
                int skippedInvalidMessages = 0;

                for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                    totalMessages++;
                    String messageId = messageSnapshot.getKey();
                    String senderId = messageSnapshot.child("sender_id").getValue(String.class);
                    String receiverId = messageSnapshot.child("receiver_id").getValue(String.class);
                    String content = messageSnapshot.child("message_content").getValue(String.class);
                    String timeStr = messageSnapshot.child("sending_time").getValue(String.class);
                    Log.d(TAG, "Processing message ID: " + messageId +
                          ", Sender: " + senderId +
                          ", Receiver: " + receiverId +
                          ", Content: " + content +
                          ", Scheduled Time: " + timeStr);
                    
                    if (messageId == null || senderId == null || receiverId == null || content == null || timeStr == null) {
                        Log.w(TAG, "Skipping invalid message with missing fields, ID: " + 
                              (messageId != null ? messageId : "unknown"));
                        skippedInvalidMessages++;
                        continue;
                    }
                    
                    try {
                        // Parse string timestamp to long
                        long scheduledTime = Long.parseLong(timeStr);
                        String formattedScheduledTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                                .format(new Date(scheduledTime));
                        
                        // Enhanced logging for each message
                        Log.d(TAG, String.format("Message ID: %s, Scheduled: %s, Current: %s, Difference: %d sec", 
                                messageId, formattedScheduledTime, formattedCurrentTime, 
                                (currentTime - scheduledTime) / 1000));
                        
                        // Check if it's time to send (use <= to ensure we don't miss any messages)
                        if (scheduledTime <= currentTime) {
                            ScheduleMessage message = new ScheduleMessage(
                                    messageId, senderId, receiverId, content, scheduledTime);
                            pendingMessages.add(message);
                            
                            Log.d(TAG, "✅ READY TO SEND: Message " + messageId + 
                                  " scheduled for " + formattedScheduledTime);
                        } else {
                            skippedFutureMessages++;
                            Log.d(TAG, "⏳ Future message: " + messageId + 
                                  " scheduled for " + formattedScheduledTime);
                        }
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Invalid timestamp format for message ID " + messageId + ": " + timeStr, e);
                        skippedInvalidMessages++;
                    }
                }

                Log.d(TAG, String.format("Summary: %d total messages, %d pending to send, %d future, %d invalid", 
                      totalMessages, pendingMessages.size(), skippedFutureMessages, skippedInvalidMessages));
                
                callback.onSuccess(pendingMessages);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                callback.onError("Failed to load pending messages: " + databaseError.getMessage());
            }
        });
    }

    /**
     * Delete a scheduled message from Firebase
     * Enhanced with more detailed error handling and logging
     */
    public void deleteScheduledMessage(String messageId, ScheduleCallback<Void> callback) {
        if (messageId == null || messageId.isEmpty()) {
            Log.e(TAG, "Cannot delete message with null or empty ID");
            callback.onError("Invalid message ID");
            return;
        }

        Log.d(TAG, "Attempting to delete scheduled message: " + messageId);

        // Verify message exists before attempting deletion
        scheduleMessagesRef.child(messageId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    Log.w(TAG, "Message " + messageId + " does not exist or was already deleted");
                    // Consider this a success since the message is already not in the database
                    callback.onSuccess(null);
                    return;
                }

                // Message exists, proceed with deletion
                scheduleMessagesRef.child(messageId).removeValue()
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Successfully deleted scheduled message: " + messageId);
                            callback.onSuccess(null);
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "Failed to delete message " + messageId + ": " + e.getMessage(), e);
                                callback.onError("Failed to delete message: " + e.getMessage());
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error checking message " + messageId + ": " + databaseError.getMessage());
                callback.onError("Failed to access message: " + databaseError.getMessage());
            }
        });
    }

    /**
     * Add a scheduled message
     */
    public void addScheduledMessage(ScheduleMessage message, String messageId, ScheduleCallback<String> callback) {
        // Create a map with the right data structure for Firebase
        Map<String, Object> messageValues = new HashMap<>();
        messageValues.put("sender_id", message.getSenderId());
        messageValues.put("receiver_id", message.getReceiverId());
        messageValues.put("message_content", message.getMessageContent());
        messageValues.put("sending_time", String.valueOf(message.getSendingTime())); // Convert long to String

        scheduleMessagesRef.child(messageId).setValue(messageValues)
                .addOnSuccessListener(aVoid -> callback.onSuccess(messageId))
                .addOnFailureListener(e -> callback.onError("Failed to add scheduled message: " + e.getMessage()));
    }

    /**
     * Generate a new message ID
     */
    public String generateMessageId() {
        return scheduleMessagesRef.push().getKey();
    }
}
