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

public class ScheduledMessageWorker extends Worker {
    private static final String TAG = "ScheduledMessageWorker";
    private final ScheduleMessageRepository repository;
    private final DatabaseReference conversationsRef;
    private final ChatFirebaseService chatService;

    public ScheduledMessageWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        repository = new ScheduleMessageRepository();
        conversationsRef = FirebaseDatabase.getInstance().getReference("conversations");
        chatService = new ChatFirebaseService();
    }

    @NonNull
    @Override
    public Result doWork() {
        // Add a small buffer to ensure we don't miss messages
        // due to slight timing differences between device and server
        long currentTime = System.currentTimeMillis();
        
        // Format time for detailed logging
        String formattedTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                .format(new Date(currentTime));
        
        Log.d(TAG, "⏰ Worker running at " + formattedTime);

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {true};

        repository.getPendingScheduledMessages(currentTime, new ScheduleMessageRepository.ScheduleCallback<List<ScheduleMessage>>() {
            @Override
            public void onSuccess(List<ScheduleMessage> messages) {
                if (messages.isEmpty()) {
                    Log.d(TAG, "No pending scheduled messages at " + formattedTime);
                    latch.countDown();
                    return;
                }
                
                Log.d(TAG, "✅ Found " + messages.size() + " messages to send now");
                
                // Process each message with detailed timing logs
                for (ScheduleMessage message : messages) {
                    String scheduledTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                            .format(new Date(message.getSendingTime()));
                    long timeDiff = currentTime - message.getSendingTime();
                    
                    Log.d(TAG, String.format("Sending message ID: %s, Content: %s, Scheduled: %s, Time diff: %d seconds",
                            message.getId(), 
                            message.getMessageContent().length() > 20 ? 
                                message.getMessageContent().substring(0, 20) + "..." : 
                                message.getMessageContent(),
                            scheduledTime,
                            timeDiff / 1000));
                }
                
                CountDownLatch sendLatch = new CountDownLatch(messages.size());

                for (ScheduleMessage message : messages) {
                    findConversationAndSendMessage(message, new SendCallback() {
                        @Override
                        public void onComplete(boolean isSuccessful) {
                            if (!isSuccessful) {
                                success[0] = false;
                                Log.e(TAG, "❌ Failed to send message: " + message.getId());
                            } else {
                                Log.d(TAG, "✅ Successfully sent scheduled message: " + message.getId());
                            }
                            sendLatch.countDown();
                        }
                    });
                }

                try {
                    // Increase timeout for large batches
                    boolean completed = sendLatch.await(30, TimeUnit.SECONDS);
                    if (!completed) {
                        Log.w(TAG, "⚠️ Timed out waiting for all messages to send");
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "Interrupted while waiting for messages to send", e);
                    success[0] = false;
                }

                latch.countDown();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error fetching scheduled messages: " + error);
                success[0] = false;
                latch.countDown();
            }
        });

        try {
            // Extended timeout for better reliability
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Worker interrupted", e);
            return Result.retry();
        }

        return success[0] ? Result.success() : Result.retry();
    }

    private interface SendCallback {
        void onComplete(boolean isSuccessful);
    }

    private void findConversationAndSendMessage(ScheduleMessage scheduleMessage, SendCallback callback) {
        String senderId = scheduleMessage.getSenderId();
        String receiverId = scheduleMessage.getReceiverId();

        // Query to find the conversation between these two users
        conversationsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String conversationId = null;

                // Look through all conversations to find one that matches these users
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String user1Id = snapshot.child("user1_id").getValue(String.class);
                    String user2Id = snapshot.child("user2_id").getValue(String.class);

                    if (user1Id != null && user2Id != null) {
                        // Check if this conversation is between our sender and receiver (in either direction)
                        if ((user1Id.equals(senderId) && user2Id.equals(receiverId)) || 
                            (user1Id.equals(receiverId) && user2Id.equals(senderId))) {
                            conversationId = snapshot.getKey();
                            break;
                        }
                    }
                }

                if (conversationId == null) {
                    Log.e(TAG, "No conversation found between users: " + senderId + " and " + receiverId);
                    callback.onComplete(false);
                    return;
                }

                // Send message using the ChatFirebaseService
                String messageContent = scheduleMessage.getMessageContent();
                String timestamp = String.valueOf(System.currentTimeMillis());
                String messageType = "text";  // Default type for scheduled messages

                chatService.sendMessage(conversationId, senderId, messageContent, messageType, timestamp, 
                    new ChatRepository.ChatCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            // Message sent successfully, now delete the scheduled message
                            repository.markScheduledMessageAsSent(scheduleMessage.getId(),
                                new ScheduleMessageRepository.ScheduleCallback<Void>() {
                                    @Override
                                    public void onSuccess(Void result) {
                                        Log.d(TAG, "Successfully sent and removed scheduled message: " + scheduleMessage.getId());
                                        callback.onComplete(true);
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Log.e(TAG, "Message sent but failed to remove from scheduled: " + error);
                                        // Consider it success since the message was sent
                                        callback.onComplete(true);
                                    }
                                });
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Log.e(TAG, "Failed to send scheduled message: " + errorMessage);
                            callback.onComplete(false);
                        }
                    });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to find conversation: " + databaseError.getMessage());
                callback.onComplete(false);
            }
        });
    }
}
