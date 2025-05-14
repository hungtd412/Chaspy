package com.example.chaspy.data.service;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.chaspy.data.model.Message;
import com.example.chaspy.data.repository.ChatRepository;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChatFirebaseService {
    private final DatabaseReference databaseRef;
    private ChildEventListener messageListener;
    private Set<String> loadedMessageIds = new HashSet<>();
    
    public ChatFirebaseService() {
        databaseRef = FirebaseDatabase.getInstance().getReference();
    }
    
    public void getMessages(String conversationId, final ChatRepository.ChatCallback<List<Message>> callback) {
        DatabaseReference messagesRef = databaseRef.child("messages").child(conversationId);
        
        messagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Message> messages = new ArrayList<>();
                loadedMessageIds.clear(); // Reset loaded message IDs
                
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String messageId = snapshot.getKey();
                    String senderId = snapshot.child("sender_id").getValue(String.class);
                    String messageContent = snapshot.child("message_content").getValue(String.class);
                    String messageType = snapshot.child("message_type").getValue(String.class);
                    String timestamp = snapshot.child("timestamp").getValue(String.class);
                    
                    if (messageId != null && senderId != null && messageContent != null && 
                        messageType != null && timestamp != null) {
                        Message message = new Message(messageId, senderId, messageContent, messageType, timestamp);
                        messages.add(message);
                        loadedMessageIds.add(messageId); // Track loaded messages
                    }
                }
                
                callback.onSuccess(messages);
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                callback.onError(databaseError.getMessage());
            }
        });
    }
    
    public void sendMessage(String conversationId, String senderId, String messageText, String messageType, 
                           String timestamp, final ChatRepository.ChatCallback<Void> callback) {
        // Create message data
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("sender_id", senderId);
        messageData.put("message_content", messageText);
        messageData.put("message_type", messageType);
        messageData.put("timestamp", timestamp);
        
        // Get references to database locations that need to be updated
        DatabaseReference messagesRef = databaseRef.child("messages").child(conversationId);
        DatabaseReference conversationsRef = databaseRef.child("conversations").child(conversationId);
        
        // Create a new message with a generated key
        DatabaseReference newMessageRef = messagesRef.push();
        String messageId = newMessageRef.getKey();
        
        if (messageId == null) {
            callback.onError("Failed to generate message ID");
            return;
        }
        
        // Write the message data
        newMessageRef.setValue(messageData)
            .addOnSuccessListener(aVoid -> {
                // Update the conversation's last message information
                Map<String, Object> conversationUpdates = new HashMap<>();
                conversationUpdates.put("last_message", messageText);
                conversationUpdates.put("last_message_time", timestamp);
                
                conversationsRef.updateChildren(conversationUpdates)
                    .addOnSuccessListener(aVoid1 -> callback.onSuccess(null))
                    .addOnFailureListener(e -> callback.onError("Failed to update conversation: " + e.getMessage()));
            })
            .addOnFailureListener(e -> callback.onError("Failed to send message: " + e.getMessage()));
    }
    
    public void listenForNewMessages(String conversationId, final ChatRepository.MessageListener listener) {
        // Remove any existing listener first
        removeMessageListener();
        
        DatabaseReference messagesRef = databaseRef.child("messages").child(conversationId);
        
        messageListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
                String messageId = dataSnapshot.getKey();
                
                // Skip if this message was already loaded in the initial fetch
                if (messageId != null && !loadedMessageIds.contains(messageId)) {
                    String senderId = dataSnapshot.child("sender_id").getValue(String.class);
                    String messageContent = dataSnapshot.child("message_content").getValue(String.class);
                    String messageType = dataSnapshot.child("message_type").getValue(String.class);
                    String timestamp = dataSnapshot.child("timestamp").getValue(String.class);
                    
                    if (senderId != null && messageContent != null && 
                        messageType != null && timestamp != null) {
                        Message message = new Message(messageId, senderId, messageContent, messageType, timestamp);
                        loadedMessageIds.add(messageId); // Add to tracked messages
                        listener.onNewMessage(message);
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
                // Handle message updates if needed
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                // Handle message deletion if needed
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
                // Handle message reordering if needed
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                listener.onError(databaseError.getMessage());
            }
        };
        
        messagesRef.addChildEventListener(messageListener);
    }
    
    public void removeMessageListener() {
        if (messageListener != null) {
            DatabaseReference messagesRef = databaseRef.child("messages");
            messagesRef.removeEventListener(messageListener);
            messageListener = null;
        }
    }
}
