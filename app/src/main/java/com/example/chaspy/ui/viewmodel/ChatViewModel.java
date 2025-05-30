package com.example.chaspy.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.chaspy.data.model.Message;
import com.example.chaspy.data.model.ScheduleMessage;
import com.example.chaspy.data.repository.ChatRepository;
import com.example.chaspy.data.repository.ScheduleMessageRepository;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatViewModel extends ViewModel {
    private final ChatRepository chatRepository;
    private final ScheduleMessageRepository scheduleMessageRepository;
    private final MutableLiveData<List<Message>> messageList = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Message> newMessageAdded = new MutableLiveData<>();
    private final MutableLiveData<List<ScheduleMessage>> scheduledMessages = new MutableLiveData<>();
    
    private String conversationId;
    private String currentUserId;
    private String friendId;
    private Map<String, Message> messageMap = new HashMap<>();
    
    public ChatViewModel() {
        chatRepository = new ChatRepository();
        scheduleMessageRepository = new ScheduleMessageRepository();
    }
    
    public void init(String conversationId, String currentUserId) {
        this.conversationId = conversationId;
        this.currentUserId = currentUserId;
        messageMap.clear(); // Clear existing messages when initializing
        loadMessages();
    }
    
    public void setFriendId(String friendId) {
        this.friendId = friendId;
    }
    
    public LiveData<List<Message>> getMessages() {
        return messageList;
    }
    
    public LiveData<String> getError() {
        return errorMessage;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<Message> getNewMessageAdded() {
        return newMessageAdded;
    }
    
    public LiveData<List<ScheduleMessage>> getScheduledMessages() {
        return scheduledMessages;
    }
    
    public void loadMessages() {
        if (conversationId == null || currentUserId == null) {
            errorMessage.setValue("Invalid conversation or user ID");
            return;
        }
        
        isLoading.setValue(true);
        
        chatRepository.getMessages(conversationId, new ChatRepository.ChatCallback<List<Message>>() {
            @Override
            public void onSuccess(List<Message> messages) {
                // Store messages in map first to prevent duplicates
                for (Message message : messages) {
                    messageMap.put(message.getMessageId(), message);
                }
                messageList.setValue(List.copyOf(messageMap.values()));
                isLoading.setValue(false);
                
                // Start listening for new messages AFTER initial load
                startMessageListener();
            }
            
            @Override
            public void onError(String error) {
                errorMessage.setValue(error);
                isLoading.setValue(false);
            }
        });
    }
    
    private void startMessageListener() {
        chatRepository.startMessageListener(conversationId, new ChatRepository.MessageListener() {
            @Override
            public void onNewMessage(Message message) {
                if (message != null && !messageMap.containsKey(message.getMessageId())) {
                    messageMap.put(message.getMessageId(), message);
                    newMessageAdded.setValue(message);
                }
            }
            
            @Override
            public void onError(String error) {
                errorMessage.setValue(error);
            }
        });
    }
    
    public void sendMessage(String messageText) {
        if (messageText == null || messageText.trim().isEmpty()) {
            errorMessage.setValue("Cannot send empty message");
            return;
        }
        
        isLoading.setValue(true);
        
        chatRepository.sendMessage(conversationId, currentUserId, messageText, new ChatRepository.ChatCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                isLoading.setValue(false);
            }
            
            @Override
            public void onError(String error) {
                errorMessage.setValue(error);
                isLoading.setValue(false);
            }
        });
    }
    
    public void loadScheduledMessages() {
        if (currentUserId == null) {
            errorMessage.setValue("User not logged in");
            return;
        }
        
        isLoading.setValue(true);
        
        scheduleMessageRepository.getScheduledMessages(currentUserId, new ScheduleMessageRepository.ScheduleCallback<List<ScheduleMessage>>() {
            @Override
            public void onSuccess(List<ScheduleMessage> messages) {
                scheduledMessages.setValue(messages);
                isLoading.setValue(false);
            }
            
            @Override
            public void onError(String error) {
                errorMessage.setValue(error);
                isLoading.setValue(false);
            }
        });
    }
    
    public void deleteScheduledMessage(String messageId) {
        if (messageId == null || messageId.isEmpty()) {
            errorMessage.setValue("Invalid message ID");
            return;
        }
        
        isLoading.setValue(true);
        
        scheduleMessageRepository.deleteScheduledMessage(messageId, new ScheduleMessageRepository.ScheduleCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // Refresh the list after deletion
                loadScheduledMessages();
                isLoading.setValue(false);
            }
            
            @Override
            public void onError(String error) {
                errorMessage.setValue(error);
                isLoading.setValue(false);
            }
        });
    }
    
    public void addScheduledMessage(String messageContent, long scheduledTime) {
        if (messageContent == null || messageContent.trim().isEmpty()) {
            errorMessage.setValue("Message content cannot be empty");
            return;
        }
        
        if (scheduledTime <= System.currentTimeMillis()) {
            errorMessage.setValue("Scheduled time must be in the future");
            return;
        }
        
        if (currentUserId == null || friendId == null) {
            errorMessage.setValue("User or friend information missing");
            return;
        }
        
        isLoading.setValue(true);
        
        // Create message with conversationId
        ScheduleMessage message = new ScheduleMessage(
                null, 
                currentUserId, 
                friendId,
                messageContent, 
                scheduledTime, 
                conversationId);  // Include conversation ID
        
        scheduleMessageRepository.addScheduledMessage(message, new ScheduleMessageRepository.ScheduleCallback<String>() {
            @Override
            public void onSuccess(String messageId) {
                // Refresh the list after addition
                loadScheduledMessages();
                isLoading.setValue(false);
            }
            
            @Override
            public void onError(String error) {
                errorMessage.setValue(error);
                isLoading.setValue(false);
            }
        });
    }

    public void updateThemeColor(String conversationId, String themeColor) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference conversationRef = database.getReference("conversations").child(conversationId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("theme_color", themeColor);

        conversationRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    // Success, nothing to do
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("Failed to update theme color: " + e.getMessage());
                });
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // Clean up resources
        chatRepository.stopMessageListener();
        messageMap.clear();
    }
}

