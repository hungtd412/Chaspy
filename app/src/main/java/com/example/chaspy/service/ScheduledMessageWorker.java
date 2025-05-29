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

public class ScheduledMessageWorker extends Worker {
    private static final String TAG = "ScheduledMessageWorker";
    private final ScheduleMessageRepository repository;
    private final DatabaseReference conversationsRef;
    private final ChatFirebaseService chatService;
    private static AtomicBoolean isRunning = new AtomicBoolean(false);
    private static long lastFullLogTime = 0;
    private static final long LOG_THROTTLE_MS = 5000;

    public ScheduledMessageWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        repository = new ScheduleMessageRepository();
        conversationsRef = FirebaseDatabase.getInstance().getReference("conversations");
        chatService = new ChatFirebaseService();
    }

    @NonNull
    @Override
    public Result doWork() {
        if (!isRunning.compareAndSet(false, true)) {
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
        }

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {true};

        repository.getPendingScheduledMessages(currentTime, new ScheduleMessageRepository.ScheduleCallback<List<ScheduleMessage>>() {
            @Override
            public void onSuccess(List<ScheduleMessage> messages) {
                if (messages.isEmpty()) {
                    if (shouldDetailLog) {
                    }
                    latch.countDown();
                    return;
                }

                for (ScheduleMessage message : messages) {
                    String scheduledTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                            .format(new Date(message.getSendingTime()));
                    long timeDiff = currentTime - message.getSendingTime();
                }

                CountDownLatch sendLatch = new CountDownLatch(messages.size());

                for (ScheduleMessage message : messages) {
                    deleteScheduledMessageBeforeSending(message, new DeleteCallback() {
                        @Override
                        public void onComplete(boolean isDeleted) {
                            if (isDeleted) {
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
                            } else {
                                success[0] = false;
                                sendLatch.countDown();
                            }
                        }
                    });
                }

                try {
                    boolean completed = sendLatch.await(10, TimeUnit.SECONDS);
                    if (!completed) {
                    }
                } catch (InterruptedException e) {
                    success[0] = false;
                }

                latch.countDown();
            }

            @Override
            public void onError(String error) {
                success[0] = false;
                latch.countDown();
            }
        });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Worker interrupted", e);
            return Result.retry();
        }

        return success[0] ? Result.success() : Result.retry();
    }

    private interface SendCallback {
        void onComplete(boolean isSuccessful);
    }

    private interface DeleteCallback {
        void onComplete(boolean isDeleted);
    }

    private void deleteScheduledMessageBeforeSending(ScheduleMessage message, DeleteCallback callback) {
        repository.deleteScheduledMessage(message.getId(), new ScheduleMessageRepository.ScheduleCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "Successfully deleted scheduled message from Firebase: " + message.getId());
                callback.onComplete(true);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to delete scheduled message: " + error + ", messageId: " + message.getId());
                callback.onComplete(false);
            }
        });
    }

    private void findConversationAndSendMessage(ScheduleMessage scheduleMessage, SendCallback callback) {
        String senderId = scheduleMessage.getSenderId();
        String receiverId = scheduleMessage.getReceiverId();
        String conversationId = scheduleMessage.getConversationId();

        if (conversationId != null && !conversationId.isEmpty()) {
            Log.d(TAG, "Using direct conversationId: " + conversationId);

            conversationsRef.child(conversationId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String user1Id = dataSnapshot.child("user1_id").getValue(String.class);
                        String user2Id = dataSnapshot.child("user2_id").getValue(String.class);

                        boolean isValidConversation = (user1Id != null && user2Id != null) &&
                                ((user1Id.equals(senderId) && user2Id.equals(receiverId)) ||
                                        (user1Id.equals(receiverId) && user2Id.equals(senderId)));

                        if (isValidConversation) {
                            sendMessageToConversation(conversationId, senderId, scheduleMessage.getMessageContent(), callback);
                        } else {
                            Log.w(TAG, "Conversation ID exists but doesn't match sender/receiver. Falling back to search.");
                            findConversationByUsers(senderId, receiverId, scheduleMessage.getMessageContent(), callback);
                        }
                    } else {
                        Log.w(TAG, "Conversation ID doesn't exist: " + conversationId + ". Falling back to search.");
                        findConversationByUsers(senderId, receiverId, scheduleMessage.getMessageContent(), callback);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Error checking conversation: " + databaseError.getMessage());
                    findConversationByUsers(senderId, receiverId, scheduleMessage.getMessageContent(), callback);
                }
            });
        } else {
            Log.d(TAG, "No conversationId provided, searching for conversation between: " + senderId + " and " + receiverId);
            findConversationByUsers(senderId, receiverId, scheduleMessage.getMessageContent(), callback);
        }
    }

    private void findConversationByUsers(String senderId, String receiverId, String messageContent, SendCallback callback) {
        conversationsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String conversationId = null;

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String user1Id = snapshot.child("user1_id").getValue(String.class);
                    String user2Id = snapshot.child("user2_id").getValue(String.class);

                    if (user1Id != null && user2Id != null) {
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

                sendMessageToConversation(conversationId, senderId, messageContent, callback);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to find conversation: " + databaseError.getMessage());
                callback.onComplete(false);
            }
        });
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
