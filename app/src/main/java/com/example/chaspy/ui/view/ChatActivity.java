package com.example.chaspy.ui.view;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chaspy.R;
import com.example.chaspy.adapter.MessageAdapter;
import com.example.chaspy.data.model.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private ImageButton buttonSend;
    private TextView textViewUsername;
    private ImageView imageViewProfile;
    
    private MessageAdapter messageAdapter;
    private LinearLayoutManager layoutManager;
    
    private String conversationId;
    private String friendUsername;
    private String friendProfilePicUrl;
    private String currentUserId;
    
    private DatabaseReference messagesRef;
    private DatabaseReference conversationsRef;
    
    // Track if we're loading initial messages
    private boolean initialMessagesLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        
        // Get data from intent
        conversationId = getIntent().getStringExtra("conversationId");
        friendUsername = getIntent().getStringExtra("friendUsername");
        friendProfilePicUrl = getIntent().getStringExtra("friendProfilePicUrl");
        
        // Get current user id
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to chat", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = currentUser.getUid();
        
        // Initialize Firebase references
        messagesRef = FirebaseDatabase.getInstance().getReference("messages").child(conversationId);
        conversationsRef = FirebaseDatabase.getInstance().getReference("conversations").child(conversationId);
        
        // Initialize UI components
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);
        textViewUsername = findViewById(R.id.textViewUsername);
        imageViewProfile = findViewById(R.id.imageViewProfile);
        
        // Setup RecyclerView
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Stack from bottom to show newest messages at bottom
        recyclerViewMessages.setLayoutManager(layoutManager);
        
        messageAdapter = new MessageAdapter(currentUserId);
        recyclerViewMessages.setAdapter(messageAdapter);
        
        // Setup UI with friend information
        textViewUsername.setText(friendUsername);
        if (friendProfilePicUrl != null && !friendProfilePicUrl.isEmpty()) {
            Picasso.get().load(friendProfilePicUrl).into(imageViewProfile);
        }
        
        // Add a back button click listener
        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
        
        // Add send button click listener
        buttonSend.setOnClickListener(v -> {
            String message = editTextMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
                editTextMessage.setText("");
            }
        });
        
        // Load messages for this conversation
        loadMessages();
    }
    
    private void loadMessages() {
        messagesRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                String messageId = dataSnapshot.getKey();
                String senderId = dataSnapshot.child("sender_id").getValue(String.class);
                String messageContent = dataSnapshot.child("message_content").getValue(String.class);
                String messageType = dataSnapshot.child("message_type").getValue(String.class);
                String timestamp = dataSnapshot.child("timestamp").getValue(String.class);
                
                if (messageId != null && senderId != null && messageContent != null && 
                    messageType != null && timestamp != null) {
                    Message message = new Message(messageId, senderId, messageContent, messageType, timestamp);
                    messageAdapter.addMessage(message);
                    scrollToBottom();
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                // Handle message updates if needed
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                // Handle message deletion if needed
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                // Handle message reordering if needed
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ChatActivity.this, "Failed to load messages: " + databaseError.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });

        // Count initial messages to know when all messages are loaded
        messagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                initialMessagesLoaded = true;
                if (dataSnapshot.exists()) {
                    scrollToBottom();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ChatActivity.this, "Failed to load message count: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void sendMessage(String messageText) {
        // Create a new message reference with a unique ID
        DatabaseReference newMessageRef = messagesRef.push();
        String messageId = newMessageRef.getKey();
        
        if (messageId == null) {
            Toast.makeText(this, "Failed to generate message ID", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Current timestamp as a string
        String timestamp = String.valueOf(System.currentTimeMillis());
        
        // Create message data
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("sender_id", currentUserId);
        messageData.put("message_content", messageText);
        messageData.put("message_type", "text");
        messageData.put("timestamp", timestamp);
        
        // Save the message
        newMessageRef.setValue(messageData)
            .addOnSuccessListener(aVoid -> {
                // Update the conversation's last message information
                Map<String, Object> conversationUpdates = new HashMap<>();
                conversationUpdates.put("last_message", messageText);
                conversationUpdates.put("last_message_time", timestamp);
                
                conversationsRef.updateChildren(conversationUpdates)
                    .addOnFailureListener(e -> Toast.makeText(ChatActivity.this, 
                        "Failed to update conversation: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            })
            .addOnFailureListener(e -> Toast.makeText(ChatActivity.this, 
                "Failed to send message: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    
    private void scrollToBottom() {
        recyclerViewMessages.post(() -> {
            int messageCount = messageAdapter.getItemCount();
            if (messageCount > 0) {
                recyclerViewMessages.smoothScrollToPosition(messageCount - 1);
            }
        });
    }
}
