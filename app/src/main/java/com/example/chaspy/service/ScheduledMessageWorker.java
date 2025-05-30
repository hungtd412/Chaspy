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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
            return performWork();
        } finally {
            isRunning.set(false);
        }
    }

    private Result performWork() {
        long currentTime = System.currentTimeMillis();
        boolean shouldDetailLog = currentTime - lastFullLogTime >= LOG_THROTTLE_MS;

        if (shouldDetailLog) {
            lastFullLogTime = currentTime;
            Log.d(TAG, "Checking for scheduled messages at " + new Date(currentTime));
        }

        // Use a shorter timeout for better responsiveness
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {true};

        repository.getPendingScheduledMessages(currentTime, new ScheduleMessageRepository.ScheduleCallback<List<ScheduleMessage>>() {
            @Override
            public void onSuccess(List<ScheduleMessage> messages) {
                if (messages == null || messages.isEmpty()) {
                    if (shouldDetailLog) {
                        Log.d(TAG, "No pending messages found");
                    }
                    latch.countDown();
                    return;
                }

                Log.d(TAG, "Processing " + messages.size() + " pending messages");

                // Process messages in parallel rather than sequentially
                CountDownLatch sendLatch = new CountDownLatch(messages.size());

                for (ScheduleMessage message : messages) {
                    // Check if this message is already being processed
                    String messageId = message.getId();
                    AtomicBoolean isProcessing = processingMessages.putIfAbsent(messageId, new AtomicBoolean(true));
                    
                    // Skip if already being processed
                    if (isProcessing != null && isProcessing.get()) {
                        Log.d(TAG, "Message " + messageId + " is already being processed, skipping");
                        sendLatch.countDown();
                        continue;
                    }
                    
                    // OPTIMIZATION: Skip deletion and check conversation first
                    // This prevents deleting messages we can't actually send
                    findConversationAndSendMessage(message, new SendCallback() {
                        @Override
                        public void onComplete(boolean isSuccessful) {
                            try {
                                if (isSuccessful) {
                                    // Only delete the message AFTER it's been successfully sent
                                    deleteScheduledMessage(message, sendLatch);
                                    Log.d(TAG, "✅ Successfully processed scheduled message: " + messageId);
                                } else {
                                    Log.e(TAG, "❌ Failed to send message: " + messageId);
                                    // Release the lock on this message so it can be tried again
                                    processingMessages.remove(messageId);
                                    success[0] = false;
                                    sendLatch.countDown();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error in completion callback: " + e.getMessage());
                                processingMessages.remove(messageId);
                                sendLatch.countDown();
                            }
                        }
                    });
                }

                // Use a shorter timeout for better responsiveness
                try {
                    boolean completed = sendLatch.await(BATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    if (!completed) {
                        Log.w(TAG, "Timed out waiting for messages to send - some operations may still be in progress");
                        success[0] = false;
                    }
                } catch (InterruptedException e) {
                    success[0] = false;
                    Log.e(TAG, "Interrupted while waiting for messages to send", e);
                }

                latch.countDown();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error fetching pending messages: " + error);
                success[0] = false;
                latch.countDown();
            }
        });

        try {
            // Use a shorter timeout
            boolean completed = latch.await(SINGLE_OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!completed) {
                Log.e(TAG, "Timed out while processing scheduled messages");
                return Result.retry();
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Worker interrupted", e);
            return Result.retry();
        }

        return success[0] ? Result.success() : Result.retry();
    }

    private interface SendCallback {
        void onComplete(boolean isSuccessful);
    }

    // Separate method for message deletion that counts down the latch
    private void deleteScheduledMessage(ScheduleMessage message, CountDownLatch latch) {
        final String messageId = message.getId();
        repository.deleteScheduledMessage(messageId, new ScheduleMessageRepository.ScheduleCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "Deleted scheduled message: " + messageId);
                // Always remove from processing map when complete
                processingMessages.remove(messageId);
                latch.countDown();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to delete scheduled message: " + error + ", messageId: " + messageId);
                // Always remove from processing map when complete
                processingMessages.remove(messageId);
                latch.countDown();
            }
        });
    }

    private void findConversationAndSendMessage(ScheduleMessage scheduleMessage, SendCallback callback) {
        String senderId = scheduleMessage.getSenderId();
        String receiverId = scheduleMessage.getReceiverId();
        String conversationId = scheduleMessage.getConversationId();
        String messageId = scheduleMessage.getId();
        
        // Ensure we never process a message twice
        AtomicBoolean messageProcessed = new AtomicBoolean(false);

        // OPTIMIZATION: If we have a conversation ID, verify it quickly and use it directly
        if (conversationId != null && !conversationId.isEmpty()) {
            Log.d(TAG, "Fast path: Using direct conversationId: " + conversationId);
            sendMessageToConversation(conversationId, senderId, scheduleMessage.getMessageContent(), 
                (success) -> {
                    // Only complete once
                    if (messageProcessed.compareAndSet(false, true)) {
                        callback.onComplete(success);
                    }
                });
            return;
        }

        // If no conversation ID, we need to search for a matching conversation
        Log.d(TAG, "Searching for conversation between: " + senderId + " and " + receiverId);
        
        final AtomicBoolean conversationFound = new AtomicBoolean(false);
        
        // Use more efficient ValueListener
        conversationsRef.orderByChild("user1_id").equalTo(senderId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean found = processConversationSnapshot(dataSnapshot, senderId, receiverId, 
                                                         scheduleMessage, messageProcessed, callback);
                conversationFound.set(found);
                
                // If not found in this query, try the other way
                if (!found) {
                    conversationsRef.orderByChild("user2_id").equalTo(senderId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            boolean secondFound = processConversationSnapshot(dataSnapshot, senderId, receiverId, 
                                                                         scheduleMessage, messageProcessed, callback);
                            
                            // If still not found after both queries, report failure
                            if (!secondFound && !messageProcessed.get()) {
                                Log.e(TAG, "No conversation found between users after both queries: " + 
                                          senderId + " and " + receiverId);
                                if (messageProcessed.compareAndSet(false, true)) {
                                    callback.onComplete(false);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.e(TAG, "Failed in second query: " + databaseError.getMessage());
                            if (messageProcessed.compareAndSet(false, true)) {
                                callback.onComplete(false);
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to query conversations by user1_id: " + databaseError.getMessage());
                // Try the other way (user might be user2)
                conversationsRef.orderByChild("user2_id").equalTo(senderId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        boolean found = processConversationSnapshot(dataSnapshot, senderId, receiverId, 
                                                                 scheduleMessage, messageProcessed, callback);
                        if (!found && !messageProcessed.get()) {
                            if (messageProcessed.compareAndSet(false, true)) {
                                callback.onComplete(false);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Failed to find conversation: " + databaseError.getMessage());
                        if (messageProcessed.compareAndSet(false, true)) {
                            callback.onComplete(false);
                        }
                    }
                });
            }
        });
    }

    // Helper method to find matching conversation from query results
    private boolean processConversationSnapshot(DataSnapshot dataSnapshot, String senderId, String receiverId,
                                          ScheduleMessage scheduleMessage, AtomicBoolean messageProcessed, 
                                          SendCallback callback) {
        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            String user1Id = snapshot.child("user1_id").getValue(String.class);
            String user2Id = snapshot.child("user2_id").getValue(String.class);

            if (user1Id != null && user2Id != null) {
                if ((user1Id.equals(senderId) && user2Id.equals(receiverId)) ||
                    (user1Id.equals(receiverId) && user2Id.equals(senderId))) {
                    
                    String conversationId = snapshot.getKey();
                    sendMessageToConversation(conversationId, senderId, scheduleMessage.getMessageContent(), 
                        (success) -> {
                            // Only call callback once
                            if (messageProcessed.compareAndSet(false, true)) {
                                callback.onComplete(success);
                            }
                        });
                    return true;
                }
            }
        }
        
        return false;
    }

    private void sendMessageToConversation(String conversationId, String senderId, String messageContent, SendCallback callback) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String messageType = "text";

        Log.d(TAG, "Sending message to conversation: " + conversationId);

        chatService.sendMessage(conversationId, senderId, messageContent, messageType, timestamp,
                new ChatRepository.ChatCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.d(TAG, "Successfully sent message to conversation: " + conversationId);
                        callback.onComplete(true);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "Failed to send scheduled message: " + errorMessage);
                        callback.onComplete(false);
                    }
                });
    }
}
