package com.example.chaspy.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.chaspy.data.model.Conversation;
import com.example.chaspy.data.repository.ConversationRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ConversationViewModel extends ViewModel {

    private MutableLiveData<List<Conversation>> conversationsLiveData;
    private MutableLiveData<String> errorLiveData;
    private ConversationRepository conversationRepository;
    private String currentUserId;
    private List<Conversation> currentConversations;
    private boolean isInitialLoadComplete = false;

    public ConversationViewModel() {
        conversationsLiveData = new MutableLiveData<>();
        errorLiveData = new MutableLiveData<>();
        conversationRepository = new ConversationRepository();
        currentConversations = new ArrayList<>();
    }

    public LiveData<List<Conversation>> getConversations() {
        return conversationsLiveData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public void loadConversations(String userId) {
        currentUserId = userId;
        
        // Reset flag when explicitly loading conversations
        isInitialLoadComplete = false;
        
        conversationRepository.getConversations(userId, new ConversationRepository.RepositoryCallback() {
            @Override
            public void onSuccess(List<Conversation> conversations) {
                currentConversations = new ArrayList<>(conversations);
                sortConversations();
                conversationsLiveData.setValue(currentConversations);
                
                // Start listening for message updates only after initial load
                // and only if we haven't done so already
                if (!isInitialLoadComplete) {
                    isInitialLoadComplete = true;
                    startMessageListener(userId);
                }
            }

            @Override
            public void onFailure(String error) {
                errorLiveData.setValue(error);
            }
        });
    }
    
    private void sortConversations() {
        Collections.sort(currentConversations, new Comparator<Conversation>() {
            @Override
            public int compare(Conversation c1, Conversation c2) {
                // Compare timestamps in reverse order (newest first)
                // First try parsing as long values for more accurate comparison
                try {
                    long time1 = Long.parseLong(c1.getLastMessageTime());
                    long time2 = Long.parseLong(c2.getLastMessageTime());
                    return Long.compare(time2, time1); // Reverse order (newest first)
                } catch (NumberFormatException e) {
                    // Fall back to string comparison if parsing fails
                    return c2.getLastMessageTime().compareTo(c1.getLastMessageTime());
                }
            }
        });
    }
    
    private void startMessageListener(String userId) {
        // Stop existing listener if any
        conversationRepository.stopMessageListener();
        
        conversationRepository.startMessageListener(userId, new ConversationRepository.MessageUpdateCallback() {
            @Override
            public void onConversationUpdated(Conversation updatedConversation) {
                updateConversationList(updatedConversation);
            }

            @Override
            public void onError(String error) {
                errorLiveData.postValue(error);
            }
        });
    }
    
    private void updateConversationList(Conversation updatedConversation) {
        // Create a new list to avoid modifying the current list during iteration
        List<Conversation> updatedList = new ArrayList<>(currentConversations.size());
        boolean found = false;
        
        // First check if this conversation already exists
        for (Conversation conversation : currentConversations) {
            if (conversation.getConversationId().equals(updatedConversation.getConversationId())) {
                // Add the updated conversation instead of the old one
                updatedList.add(updatedConversation);
                found = true;
            } else {
                // Keep other conversations as they are
                updatedList.add(conversation);
            }
        }
        
        // If it's a new conversation, add it to the list
        if (!found) {
            updatedList.add(updatedConversation);
        }
        
        // Update the current list reference
        currentConversations = updatedList;
        
        // Sort conversations by timestamp (newest first)
        sortConversations();
        
        // Update the LiveData with a new list to trigger observers
        conversationsLiveData.postValue(new ArrayList<>(currentConversations));
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // Clean up the message listener when the ViewModel is cleared
        conversationRepository.stopMessageListener();
    }
}
