package com.example.chaspy.service;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.chaspy.data.model.ScheduleMessage;
import com.example.chaspy.data.repository.ScheduleMessageRepository;
import com.example.chaspy.data.repository.ChatRepository;
import com.example.chaspy.data.service.ChatFirebaseService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;

public class ScheduledMessageWorker extends Worker {
    private static final String TAG = "ScheduledMessageWorker";
    private final ScheduleMessageRepository repository;
    private final DatabaseReference conversationsRef;
    private final ChatFirebaseService chatService;
    private static AtomicBoolean isRunning = new AtomicBoolean(false);
    private static long lastFullLogTime = 0;
    private static final long LOG_THROTTLE_MS = 5000;
    // Add timeout constants
    private static final int BATCH_TIMEOUT_SECONDS = 10;
    private static final int SINGLE_OPERATION_TIMEOUT_SECONDS = 5;
    // Add a processing tracker to prevent duplicate sends
    private static final ConcurrentHashMap<String, AtomicBoolean> processingMessages = new ConcurrentHashMap<>();

    public ScheduledMessageWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        repository = new ScheduleMessageRepository();
        conversationsRef = FirebaseDatabase.getInstance().getReference("conversations");
        chatService = new ChatFirebaseService();
    }

    @NonNull
    @Override
    public Result doWork() {
        // Use compareAndSet to ensure only one worker runs at a time
        if (!isRunning.compareAndSet(false, true)) {
            Log.d(TAG, "Worker already running, skipping this execution");
            return Result.success();
        }

        try {
            // Run the work in the same thread (WorkManager already provides a background thread)
            return performWork();
        } finally {
            isRunning.set(false);
        }
    }

    private Result performWork() {
        // Use atomic boolean to track success status
        AtomicBoolean successFlag = new AtomicBoolean(true);
        
        // Get current timestamp for checking scheduled messages
        long currentTimeMillis = System.currentTimeMillis();
        
        // Log worker execution (throttled)
        if (currentTimeMillis - lastFullLogTime > LOG_THROTTLE_MS) {
            Log.d(TAG, "ScheduledMessageWorker checking for messages to send at " + 
                   new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date()));
            lastFullLogTime = currentTimeMillis;
        }

        // Since we're already on a background thread provided by WorkManager,
        // we can fetch messages and process them sequentially
        try {
            // Instead of using CountDownLatch, we can use a blocking get() here
            // since we are already on a WorkManager background thread
            repository.getPendingScheduledMessagesBlocking(currentTimeMillis, new ScheduleMessageRepository.ScheduleCallback<List<ScheduleMessage>>() {
                @Override
                public void onSuccess(List<ScheduleMessage> messages) {
                    if (messages == null || messages.isEmpty()) {
                        // No messages to send
                        return;
                    }
                    
                    // Process each message sequentially
                    for (ScheduleMessage message : messages) {
                        try {
                            // Process this message
                            processScheduledMessage(message);
                        } catch (Exception e) {
                            // Log error but continue with other messages
                            Log.e(TAG, "Error processing scheduled message: " + e.getMessage(), e);
                            successFlag.set(false);
                        }
                    }
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Error fetching scheduled messages: " + error);
                    successFlag.set(false);
                }
            });
            
            // Return appropriate result based on success flag
            return successFlag.get() ? Result.success() : Result.retry();
            
        } catch (Exception e) {
            Log.e(TAG, "Exception in worker thread: " + e.getMessage(), e);
            return Result.retry();
        }
    }

    // Process a single scheduled message
    private void processScheduledMessage(ScheduleMessage message) {
        String messageId = message.getId();
        
        // Skip if we're already processing this message
        if (!processingMessages.computeIfAbsent(messageId, k -> new AtomicBoolean(false)).compareAndSet(false, true)) {
            Log.d(TAG, "Already processing message: " + messageId + ", skipping");
            return;
        }
        
        try {
            String senderId = message.getSenderId();
            String receiverId = message.getReceiverId();
            String conversationId = message.getConversationId();
            
            Log.d(TAG, "Processing scheduled message: " + messageId + " for conversation: " + conversationId);

            if (conversationId != null && !conversationId.isEmpty()) {
                // We know the conversation ID, send directly
                sendMessageAndCleanup(conversationId, message);
            } else {
                // Need to find the conversation ID first
                findConversationAndSendMessage(message);
            }
        } finally {
            // Remove from processing map when done
            processingMessages.remove(messageId);
        }
    }

    private void findConversationAndSendMessage(ScheduleMessage message) {
        String senderId = message.getSenderId();
        String receiverId = message.getReceiverId();
        
        // First check where sender is user1
        conversationsRef.orderByChild("user1_id").equalTo(senderId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (!findAndProcessConversation(dataSnapshot, senderId, receiverId, message)) {
                        // If not found, check where sender is user2
                        conversationsRef.orderByChild("user2_id").equalTo(senderId)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (!findAndProcessConversation(dataSnapshot, senderId, receiverId, message)) {
                                        // Conversation not found
                                        Log.e(TAG, "No conversation found for sender: " + senderId + 
                                             " and receiver: " + receiverId);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    Log.e(TAG, "Database error checking user2_id: " + databaseError.getMessage());
                                }
                            });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Database error checking user1_id: " + databaseError.getMessage());
                }
            });
    }

    // Find matching conversation in snapshot and process the message
    private boolean findAndProcessConversation(DataSnapshot dataSnapshot, 
                                              String senderId, 
                                              String receiverId,
                                              ScheduleMessage message) {
        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            String user1Id = snapshot.child("user1_id").getValue(String.class);
            String user2Id = snapshot.child("user2_id").getValue(String.class);

            if (user1Id != null && user2Id != null) {
                if ((user1Id.equals(senderId) && user2Id.equals(receiverId)) ||
                    (user1Id.equals(receiverId) && user2Id.equals(senderId))) {

                    String conversationId = snapshot.getKey();
                    sendMessageAndCleanup(conversationId, message);
                    return true;
                }
            }
        }
        return false;
    }

    // Send the message and then delete the scheduled message entry
    private void sendMessageAndCleanup(String conversationId, ScheduleMessage message) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String messageType = "text";
        String senderId = message.getSenderId();
        String messageContent = message.getMessageContent();
        
        Log.d(TAG, "Sending scheduled message to conversation: " + conversationId);
        
        chatService.sendMessage(conversationId, senderId, messageContent, messageType, timestamp,
            new ChatRepository.ChatCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d(TAG, "Successfully sent scheduled message to conversation: " + conversationId);
                    
                    // Delete the scheduled message after successful send
                    repository.deleteScheduledMessage(message.getId(), new ScheduleMessageRepository.ScheduleCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Log.d(TAG, "Deleted scheduled message: " + message.getId());
                        }
                        
                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Failed to delete scheduled message after sending: " + error);
                        }
                    });
                }
                
                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "Failed to send scheduled message: " + errorMessage);
                }
            });
    }

    private interface SendCallback {
        void onComplete(boolean isSuccessful);
    }
}
