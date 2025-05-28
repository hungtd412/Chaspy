package com.example.chaspy.data.service;

import com.example.chaspy.data.model.Conversation;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;

public class ConversationFirebaseService {
    private final DatabaseReference conversationsRef;
    private final DatabaseReference usersRef;

    public ConversationFirebaseService() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        conversationsRef = database.getReference("conversations");
        usersRef = database.getReference("users");
    }

    public void getConversations(String userId, FirebaseCallback callback) {
        conversationsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Conversation> conversations = new ArrayList<>();
                List<String> pendingConversationIds = new ArrayList<>();
                
                for (DataSnapshot conversationSnapshot : snapshot.getChildren()) {
                    String user1Id = conversationSnapshot.child("user1_id").getValue(String.class);
                    String user2Id = conversationSnapshot.child("user2_id").getValue(String.class);
                    
                    // Check if this conversation involves the current user
                    if (userId.equals(user1Id) || userId.equals(user2Id)) {
                        String conversationId = conversationSnapshot.getKey();
                        String lastMessage = conversationSnapshot.child("last_message").getValue(String.class);
                        String lastMessageTime = conversationSnapshot.child("last_message_time").getValue(String.class);
                        
                        // Skip conversations with null or empty last messages
                        if (lastMessage == null || lastMessage.trim().isEmpty()) {
                            continue;
                        }
                        
                        // Determine the friend's ID (the other user in the conversation)
                        final String friendId = userId.equals(user1Id) ? user2Id : user1Id;
                        
                        // Check if they are still friends before adding the conversation
                        checkFriendshipStatus(userId, friendId, new FriendshipStatusCallback() {
                            @Override
                            public void onResult(boolean areFriends) {
                                // Only process if they are still friends
                                if (areFriends) {
                                    // Create a conversation object with temporary empty values for friend details
                                    Conversation conversation = new Conversation(
                                            conversationId,
                                            lastMessage, 
                                            lastMessageTime,
                                            friendId,
                                            "", // Temporary empty friend username
                                            "" // Temporary empty profile pic URL
                                    );
                                    
                                    conversations.add(conversation);
                                    pendingConversationIds.add(conversationId);
                                    
                                    // Fetch the friend's details from the users node
                                    usersRef.child(friendId).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if (dataSnapshot.exists()) {
                                                String firstName = dataSnapshot.child("firstName").getValue(String.class);
                                                String lastName = dataSnapshot.child("lastName").getValue(String.class);
                                                String friendUsername = firstName + " " + lastName;
                                                String profilePicUrl = dataSnapshot.child("profilePicUrl").getValue(String.class);
                                                
                                                // Update the conversation with the friend's details
                                                conversation.setFriendUsername(friendUsername);
                                                conversation.setProfilePicUrl(profilePicUrl);
                                                
                                                // Remove from pending list
                                                pendingConversationIds.remove(conversationId);
                                                
                                                // If all conversations have been updated, notify callback
                                                if (pendingConversationIds.isEmpty()) {
                                                    callback.onSuccess(conversations);
                                                }
                                            }
                                        }
                                        
                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            callback.onFailure(error.getMessage());
                                        }
                                    });
                                } else {
                                    // If not friends, check if this was the last pending conversation
                                    if (pendingConversationIds.isEmpty() && conversations.isEmpty()) {
                                        callback.onSuccess(conversations);
                                    }
                                }
                            }
                        });
                    }
                }
                
                // If no conversations found
                if (pendingConversationIds.isEmpty() && conversations.isEmpty()) {
                    callback.onSuccess(conversations);
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailure(error.getMessage());
            }
        });
    }
    
    public void getSingleConversationWithDetails(String conversationId, String currentUserId, SingleConversationCallback callback) {
        conversationsRef.child(conversationId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot conversationSnapshot) {
                if (conversationSnapshot.exists()) {
                    String user1Id = conversationSnapshot.child("user1_id").getValue(String.class);
                    String user2Id = conversationSnapshot.child("user2_id").getValue(String.class);
                    String lastMessage = conversationSnapshot.child("last_message").getValue(String.class);
                    String lastMessageTime = conversationSnapshot.child("last_message_time").getValue(String.class);
                    
                    // Skip conversations with null or empty last messages
                    if (lastMessage == null || lastMessage.trim().isEmpty()) {
                        callback.onFailure("Empty last message");
                        return;
                    }
                    
                    // Determine friend ID
                    String friendId = currentUserId.equals(user1Id) ? user2Id : user1Id;
                    
                    // Check if they are still friends before proceeding
                    checkFriendshipStatus(currentUserId, friendId, new FriendshipStatusCallback() {
                        @Override
                        public void onResult(boolean areFriends) {
                            if (areFriends) {
                                // Create a conversation object with temporary values
                                Conversation conversation = new Conversation(
                                        conversationId,
                                        lastMessage,
                                        lastMessageTime,
                                        friendId,
                                        "",
                                        ""
                                );
                                
                                // Get friend details
                                usersRef.child(friendId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                        if (userSnapshot.exists()) {
                                            String firstName = userSnapshot.child("firstName").getValue(String.class);
                                            String lastName = userSnapshot.child("lastName").getValue(String.class);
                                            String friendUsername = firstName + " " + lastName;
                                            String profilePicUrl = userSnapshot.child("profilePicUrl").getValue(String.class);
                                            
                                            // Update the conversation with friend details
                                            conversation.setFriendUsername(friendUsername);
                                            conversation.setProfilePicUrl(profilePicUrl);
                                            
                                            callback.onSuccess(conversation);
                                        } else {
                                            callback.onFailure("User not found");
                                        }
                                    }
                                    
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        callback.onFailure(error.getMessage());
                                    }
                                });
                            } else {
                                callback.onFailure("Users are not friends");
                            }
                        }
                    });
                } else {
                    callback.onFailure("Conversation not found");
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailure(error.getMessage());
            }
        });
    }
    
    // Helper method to check if two users are friends
    private void checkFriendshipStatus(String userId, String friendId, FriendshipStatusCallback callback) {
        usersRef.child(userId).child("friends").child(friendId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // If the value exists and is true, they are friends
                boolean areFriends = snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                callback.onResult(areFriends);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Default to not friends on error
                callback.onResult(false);
            }
        });
    }
    
    public void updateConversation(String conversationId, String lastMessage, String timestamp) {
        // Skip updating if the message is empty
        if (lastMessage == null || lastMessage.trim().isEmpty()) {
            return;
        }
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("last_message", lastMessage);
        updates.put("last_message_time", timestamp);
        
        conversationsRef.child(conversationId).updateChildren(updates);
    }

    public interface FirebaseCallback {
        void onSuccess(List<Conversation> conversations);
        void onFailure(String error);
    }
    
    public interface SingleConversationCallback {
        void onSuccess(Conversation conversation);
        void onFailure(String error);
    }
    
    public interface FriendshipStatusCallback {
        void onResult(boolean areFriends);
    }
}
