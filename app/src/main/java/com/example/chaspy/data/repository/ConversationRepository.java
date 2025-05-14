package com.example.chaspy.data.repository;

import com.example.chaspy.data.model.Conversation;
import com.example.chaspy.data.service.ConversationFirebaseService;
import com.example.chaspy.data.service.MessageListener;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ConversationRepository {

    private ConversationFirebaseService conversationService;
    private MessageListener messageListener;
    private boolean isListening = false;

    public ConversationRepository() {
        conversationService = new ConversationFirebaseService();
        messageListener = new MessageListener();
    }

    public void getConversations(String userId, final RepositoryCallback callback) {
        conversationService.getConversations(userId, new ConversationFirebaseService.FirebaseCallback() {
            @Override
            public void onSuccess(List<Conversation> conversations) {
                // Sort conversations by time (newest first)
                Collections.sort(conversations, new Comparator<Conversation>() {
                    @Override
                    public int compare(Conversation c1, Conversation c2) {
                        try {
                            // Try to parse as long for more accurate comparison
                            long time1 = Long.parseLong(c1.getLastMessageTime());
                            long time2 = Long.parseLong(c2.getLastMessageTime());
                            return Long.compare(time2, time1); // Reverse order (newest first)
                        } catch (NumberFormatException e) {
                            // Fall back to string comparison
                            return c2.getLastMessageTime().compareTo(c1.getLastMessageTime());
                        }
                    }
                });
                callback.onSuccess(conversations);
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure(error);
            }
        });
    }

    public void startMessageListener(String userId, final MessageUpdateCallback callback) {
        // Prevent multiple listeners
        if (isListening) {
            stopMessageListener();
        }
        
        isListening = true;
        messageListener.startListening(userId, new MessageListener.MessageUpdateCallback() {
            @Override
            public void onNewMessage(String conversationId, String message, String timestamp, String senderId) {
                // Update the conversation in Firebase
                conversationService.updateConversation(conversationId, message, timestamp);
                
                // Get user info for the updated conversation
                conversationService.getSingleConversationWithDetails(conversationId, userId, 
                    new ConversationFirebaseService.SingleConversationCallback() {
                        @Override
                        public void onSuccess(Conversation conversation) {
                            // Notify the callback with complete conversation data
                            callback.onConversationUpdated(conversation);
                        }

                        @Override
                        public void onFailure(String error) {
                            callback.onError(error);
                        }
                    });
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
                isListening = false;
            }
        });
    }

    public void stopMessageListener() {
        messageListener.stopListening();
        isListening = false;
    }

    public interface RepositoryCallback {
        void onSuccess(List<Conversation> conversations);
        void onFailure(String error);
    }
    
    public interface MessageUpdateCallback {
        void onConversationUpdated(Conversation conversation);
        void onError(String error);
    }
}
