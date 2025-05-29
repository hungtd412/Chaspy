package com.example.chaspy.data.repository;

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

public class ScheduleMessageRepository {
    
    private final DatabaseReference scheduleMessagesRef;
    
    public ScheduleMessageRepository() {
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
     * Delete a scheduled message
     */
    public void deleteScheduledMessage(String messageId, ScheduleCallback<Void> callback) {
        scheduleMessagesRef.child(messageId).removeValue()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onError("Failed to delete message: " + e.getMessage());
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
            messageId = scheduleMessagesRef.push().getKey();
        }
        
        if (messageId == null) {
            callback.onError("Failed to generate message ID");
            return;
        }
        
        // Create a final copy of the ID for use in the lambda
        final String finalMessageId = messageId;
        
        message.setId(finalMessageId);
        
        // Create a map with the right data structure for Firebase
        Map<String, Object> messageValues = new HashMap<>();
        messageValues.put("sender_id", message.getSenderId());
        messageValues.put("receiver_id", message.getReceiverId());
        messageValues.put("message_content", message.getMessageContent());
        messageValues.put("sending_time", String.valueOf(message.getSendingTime())); // Convert long to String
        
        scheduleMessagesRef.child(finalMessageId).setValue(messageValues)
                .addOnSuccessListener(aVoid -> callback.onSuccess(finalMessageId))
                .addOnFailureListener(e -> callback.onError("Failed to add scheduled message: " + e.getMessage()));
    }
}
