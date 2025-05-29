package com.example.chaspy.data.service;

import androidx.annotation.NonNull;

import com.example.chaspy.data.model.ScheduleMessage;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.Query;

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
                        String conversationId = messageSnapshot.child("conversation_id").getValue(String.class);

                        if (receiverId != null && content != null && timeStr != null) {
                            try {
                                // Parse string timestamp to long
                                long time = Long.parseLong(timeStr);
                                ScheduleMessage scheduleMessage = new ScheduleMessage(
                                        messageId, senderId, receiverId, content, time, conversationId);
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
     * OPTIMIZED with indexed query to reduce data transfer and processing time
     */
    public void getPendingScheduledMessages(long currentTime, ScheduleCallback<List<ScheduleMessage>> callback) {
        // Format current time for logging
        String formattedCurrentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                .format(new Date(currentTime));
        
        Log.d(TAG, "Checking for pending messages at current time: " + formattedCurrentTime);
        
        // OPTIMIZATION: Using query to filter data server-side instead of client-side
        // This greatly reduces data transfer and processing time
        String currentTimeStr = String.valueOf(currentTime);
        
        // Query messages where sending_time is less than or equal to current time
        // Note: string comparison works because Firebase stores timestamps as strings
        // and lexicographical string comparison matches numeric order for same-length numbers
        Query pendingMessagesQuery = scheduleMessagesRef.orderByChild("sending_time")
                                                        .endAt(currentTimeStr);
                                                        
        pendingMessagesQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<ScheduleMessage> pendingMessages = new ArrayList<>();
                int totalMessages = 0;
                int validMessages = 0;
                int invalidMessages = 0;

                for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                    totalMessages++;
                    String messageId = messageSnapshot.getKey();
                    String senderId = messageSnapshot.child("sender_id").getValue(String.class);
                    String receiverId = messageSnapshot.child("receiver_id").getValue(String.class);
                    String content = messageSnapshot.child("message_content").getValue(String.class);
                    String timeStr = messageSnapshot.child("sending_time").getValue(String.class);
                    String conversationId = messageSnapshot.child("conversation_id").getValue(String.class);
                    
                    if (messageId == null || senderId == null || receiverId == null || content == null || timeStr == null) {
                        invalidMessages++;
                        continue;
                    }
                    
                    try {
                        // Parse string timestamp to long
                        long scheduledTime = Long.parseLong(timeStr);
                        
                        // Add the message to the list of pending messages
                        ScheduleMessage message = new ScheduleMessage(
                                messageId, senderId, receiverId, content, scheduledTime, conversationId);
                        pendingMessages.add(message);
                        validMessages++;
                    } catch (NumberFormatException e) {
                        invalidMessages++;
                    }
                }

                Log.d(TAG, String.format("[PERFORMANCE] Query returned %d total messages, %d valid pending, %d invalid", 
                        totalMessages, validMessages, invalidMessages));
                
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
     * Optimized for performance
     */
    public void deleteScheduledMessage(String messageId, ScheduleCallback<Void> callback) {
        if (messageId == null || messageId.isEmpty()) {
            Log.e(TAG, "Cannot delete message with null or empty ID");
            callback.onError("Invalid message ID");
            return;
        }

        // Directly delete without checking if it exists first to save a network call
        scheduleMessagesRef.child(messageId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Deleted scheduled message: " + messageId);
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
        
        // Add conversation_id if available
        if (message.getConversationId() != null && !message.getConversationId().isEmpty()) {
            messageValues.put("conversation_id", message.getConversationId());
        }

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
