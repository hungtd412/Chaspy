package com.example.chaspy.data.repository;

import com.example.chaspy.data.model.Message;
import com.example.chaspy.data.service.ChatFirebaseService;

import java.util.List;

public class ChatRepository {
    private final ChatFirebaseService firebaseService;
    
    public ChatRepository() {
        firebaseService = new ChatFirebaseService();
    }
    
    public void getMessages(String conversationId, ChatCallback<List<Message>> callback) {
        firebaseService.getMessages(conversationId, callback);
    }
    
    public void sendMessage(String conversationId, String senderId, String messageText, ChatCallback<Void> callback) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        firebaseService.sendMessage(conversationId, senderId, messageText, "text", timestamp, callback);
    }
    
    public void startMessageListener(String conversationId, MessageListener listener) {
        firebaseService.listenForNewMessages(conversationId, listener);
    }
    
    public void stopMessageListener() {
        firebaseService.removeMessageListener();
    }
    
    public interface ChatCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }
    
    public interface MessageListener {
        void onNewMessage(Message message);
        void onError(String error);
    }
}
