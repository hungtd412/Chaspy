package com.example.chaspy.data.repository;

import com.example.chaspy.data.model.Conversation;
import com.example.chaspy.data.service.ConversationFirebaseService;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ConversationRepository {

    private ConversationFirebaseService conversationService;

    public ConversationRepository() {
        conversationService = new ConversationFirebaseService();
    }

    public void getConversations(String userId, final RepositoryCallback callback) {
        conversationService.getConversations(userId, new ConversationFirebaseService.FirebaseCallback() {
            @Override
            public void onSuccess(List<Conversation> conversations) {
                // Sort conversations by time (newest first)
                Collections.sort(conversations, new Comparator<Conversation>() {
                    @Override
                    public int compare(Conversation c1, Conversation c2) {
                        // For String timestamps, compare them in reverse order
                        return c2.getLastMessageTime().compareTo(c1.getLastMessageTime());
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

    public interface RepositoryCallback {
        void onSuccess(List<Conversation> conversations);
        void onFailure(String error);
    }
}
