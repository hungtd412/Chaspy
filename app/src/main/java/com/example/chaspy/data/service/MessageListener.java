package com.example.chaspy.data.service;

import androidx.annotation.NonNull;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class MessageListener {
    private DatabaseReference conversationsRef;
    private DatabaseReference messagesRef;
    private Map<String, ChildEventListener> messageListeners;
    private ValueEventListener conversationsListener;

    public MessageListener() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        conversationsRef = database.getReference("conversations");
        messagesRef = database.getReference("messages");
        messageListeners = new HashMap<>();
    }

    public void startListening(String userId, final MessageUpdateCallback callback) {
        // Listen for changes to conversations involving the user
        conversationsListener = conversationsRef.addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot conversationSnapshot : snapshot.getChildren()) {
                            String user1Id = conversationSnapshot.child("user1_id").getValue(String.class);
                            String user2Id = conversationSnapshot.child("user2_id").getValue(String.class);
                            
                            // Only proceed if this conversation involves the current user
                            if (userId.equals(user1Id) || userId.equals(user2Id)) {
                                String conversationId = conversationSnapshot.getKey();
                                String lastMessage = conversationSnapshot.child("last_message").getValue(String.class);
                                String lastMessageTime = conversationSnapshot.child("last_message_time").getValue(String.class);
                                
                                // The other user in the conversation
                                String senderId = userId.equals(user1Id) ? user2Id : user1Id;
                                
                                // Notify about the conversation update
                                callback.onNewMessage(conversationId, lastMessage, lastMessageTime, senderId);
                                
                                // Ensure we're listening to this conversation's messages
                                if (!messageListeners.containsKey(conversationId)) {
                                    setupMessageListenerForConversation(conversationId, userId, callback);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error.getMessage());
                    }
                }
        );
    }
    
    private void setupMessageListenerForConversation(String conversationId, String userId, final MessageUpdateCallback callback) {
        // Set up a listener for new messages in this conversation - use limitToLast(1) to get only the latest message
        Query lastMessageQuery = messagesRef.child(conversationId).orderByChild("timestamp").limitToLast(1);
        ChildEventListener messageListener = lastMessageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String previousChildName) {
                if (dataSnapshot.exists()) {
                    processMessageUpdate(dataSnapshot, conversationId, userId, callback);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String previousChildName) {
                // Message content changed (unlikely, but possible)
                processMessageUpdate(dataSnapshot, conversationId, userId, callback);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                // Not handling message deletion
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, String previousChildName) {
                // Messages don't move in our structure
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                callback.onError(databaseError.getMessage());
            }
        });
        
        // Store the listener for later removal
        messageListeners.put(conversationId, messageListener);
    }
    
    private void processMessageUpdate(DataSnapshot dataSnapshot, String conversationId, String userId, MessageUpdateCallback callback) {
        String messageContent = dataSnapshot.child("message_content").getValue(String.class);
        String timestamp = dataSnapshot.child("timestamp").getValue(String.class);
        String senderId = dataSnapshot.child("sender_id").getValue(String.class);
        
        if (messageContent != null && timestamp != null && senderId != null) {
            // Check if this is a new message by comparing timestamps
            // Update conversation metadata in Firebase
            Map<String, Object> updates = new HashMap<>();
            updates.put("last_message", messageContent);
            updates.put("last_message_time", timestamp);
            conversationsRef.child(conversationId).updateChildren(updates);
            
            // Notify about the new message - this will trigger sorting in the ViewModel
            callback.onNewMessage(conversationId, messageContent, timestamp, senderId);
        }
    }

    public void stopListening() {
        // Remove the conversations listener
        if (conversationsListener != null) {
            conversationsRef.removeEventListener(conversationsListener);
            conversationsListener = null;
        }
        
        // Remove all message listeners
        for (Map.Entry<String, ChildEventListener> entry : messageListeners.entrySet()) {
            messagesRef.child(entry.getKey()).removeEventListener(entry.getValue());
        }
        messageListeners.clear();
    }

    public interface MessageUpdateCallback {
        void onNewMessage(String conversationId, String message, String timestamp, String senderId);
        void onError(String error);
    }
}
