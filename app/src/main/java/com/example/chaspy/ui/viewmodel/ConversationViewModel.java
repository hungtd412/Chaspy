package com.example.chaspy.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.chaspy.data.model.Conversation;
import com.example.chaspy.data.repository.ConversationRepository;

import java.util.List;

public class ConversationViewModel extends ViewModel {

    private MutableLiveData<List<Conversation>> conversationsLiveData;
    private MutableLiveData<String> errorLiveData;
    private ConversationRepository conversationRepository;

    public ConversationViewModel() {
        conversationsLiveData = new MutableLiveData<>();
        errorLiveData = new MutableLiveData<>();
        conversationRepository = new ConversationRepository();
    }

    public LiveData<List<Conversation>> getConversations() {
        return conversationsLiveData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public void loadConversations(String userId) {
        conversationRepository.getConversations(userId, new ConversationRepository.RepositoryCallback() {
            @Override
            public void onSuccess(List<Conversation> conversations) {
                conversationsLiveData.setValue(conversations);
            }

            @Override
            public void onFailure(String error) {
                errorLiveData.setValue(error);
            }
        });
    }
}

