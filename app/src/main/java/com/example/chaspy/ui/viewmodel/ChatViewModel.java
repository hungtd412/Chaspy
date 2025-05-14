package com.example.chaspy.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.chaspy.data.model.Message;
import com.example.chaspy.data.repository.ChatRepository;

import java.util.List;

public class ChatViewModel extends ViewModel {
    private final ChatRepository chatRepository;
    private final MutableLiveData<List<Message>> messageList = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Message> newMessageAdded = new MutableLiveData<>();
    
    private String conversationId;
    private String currentUserId;
    
    public ChatViewModel() {
        chatRepository = new ChatRepository();
    }
    
    public void init(String conversationId, String currentUserId) {
        this.conversationId = conversationId;
        this.currentUserId = currentUserId;
        loadMessages();
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
    
    public void loadMessages() {
        if (conversationId == null || currentUserId == null) {
            errorMessage.setValue("Invalid conversation or user ID");
            return;
        }
        
        isLoading.setValue(true);
        
        chatRepository.getMessages(conversationId, new ChatRepository.ChatCallback<List<Message>>() {
            @Override
            public void onSuccess(List<Message> messages) {
                messageList.setValue(messages);
                isLoading.setValue(false);
            }
            
            @Override
            public void onError(String error) {
                errorMessage.setValue(error);
                isLoading.setValue(false);
            }
        });
        
        // Start listening for new messages
        chatRepository.startMessageListener(conversationId, new ChatRepository.MessageListener() {
            @Override
            public void onNewMessage(Message message) {
                newMessageAdded.setValue(message);
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
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // Clean up resources
        chatRepository.stopMessageListener();
    }
}
