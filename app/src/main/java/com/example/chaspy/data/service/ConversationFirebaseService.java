package com.example.chaspy.data.service;

import android.util.Log;

import com.example.chaspy.data.model.Conversation;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ConversationFirebaseService {

    private FirebaseDatabase database;

    public ConversationFirebaseService() {
        database = FirebaseDatabase.getInstance();
    }

    public void getConversations(String userId, final FirebaseCallback callback) {
        database.getReference("conversations")
                .orderByChild("last_message_time")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        List<DataSnapshot> matchingConversations = new ArrayList<>();
                        
                        // First, collect all conversation snapshots that involve this user
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String user1Id = snapshot.child("user1_id").getValue(String.class);
                            String user2Id = snapshot.child("user2_id").getValue(String.class);

                            if (user1Id != null && user2Id != null &&
                                (user1Id.equals(userId) || user2Id.equals(userId))) {
                                matchingConversations.add(snapshot);
                            }
                        }
                        
                        // If no conversations found
                        if (matchingConversations.isEmpty()) {
                            callback.onSuccess(new ArrayList<>());
                            return;
                        }
                        
                        // Process all matching conversations
                        List<Conversation> conversations = new ArrayList<>();
                        AtomicInteger pendingRequests = new AtomicInteger(matchingConversations.size());
                        
                        for (DataSnapshot snapshot : matchingConversations) {
                            String user1Id = snapshot.child("user1_id").getValue(String.class);
                            String user2Id = snapshot.child("user2_id").getValue(String.class);
                            String friendId = user1Id.equals(userId) ? user2Id : user1Id;
                            
                            // Fetch friend details for each conversation
                            database.getReference("users")
                                .child(friendId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot userSnapshot) {
                                        String friendUsername = userSnapshot.child("first_name").getValue(String.class);
                                        String profilePicUrl = userSnapshot.child("profilePicUrl").getValue(String.class);

                                        // Create conversation object
                                        Conversation conversation = new Conversation(
                                            snapshot.getKey(),
                                            friendUsername,
                                            profilePicUrl,
                                            snapshot.child("last_message").getValue(String.class),
                                            snapshot.child("last_message_time").getValue(String.class)
                                        );

                                        // Add to our result list
                                        conversations.add(conversation);
                                        
                                        // If this was the last pending request, return all results
                                        if (pendingRequests.decrementAndGet() == 0) {
                                            callback.onSuccess(conversations);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        if (pendingRequests.decrementAndGet() == 0) {
                                            // Even if one fails, return what we have
                                            callback.onSuccess(conversations);
                                        }
                                    }
                                });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        callback.onFailure(databaseError.getMessage());
                    }
                });
    }

    public interface FirebaseCallback {
        void onSuccess(List<Conversation> conversations);
        void onFailure(String error);
    }
}
