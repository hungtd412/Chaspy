package com.example.chaspy.ui.view;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chaspy.R;
import com.example.chaspy.adapter.MessageAdapter;
import com.example.chaspy.data.model.Message;
import com.example.chaspy.ui.viewmodel.ChatViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private ImageButton buttonSend;
    private TextView textViewUsername;
    private ImageView imageViewProfile;
    
    private MessageAdapter messageAdapter;
    private LinearLayoutManager layoutManager;
    private ChatViewModel chatViewModel;
    
    private String conversationId;
    private String friendUsername;
    private String friendProfilePicUrl;
    private String currentUserId;

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
        
        // Initialize ViewModel
        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        chatViewModel.init(conversationId, currentUserId);
        
        // Observe messages
        chatViewModel.getMessages().observe(this, new Observer<List<Message>>() {
            @Override
            public void onChanged(List<Message> messages) {
                messageAdapter.setMessages(messages);
                scrollToBottom();
            }
        });
        
        // Observe new messages
        chatViewModel.getNewMessageAdded().observe(this, new Observer<Message>() {
            @Override
            public void onChanged(Message message) {
                if (message != null) {
                    messageAdapter.addMessage(message);
                    scrollToBottom();
                }
            }
        });
        
        // Observe errors
        chatViewModel.getError().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String error) {
                if (error != null && !error.isEmpty()) {
                    Toast.makeText(ChatActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        // Add send button click listener
        buttonSend.setOnClickListener(v -> {
            String message = editTextMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                chatViewModel.sendMessage(message);
                editTextMessage.setText("");
            }
        });
    }
    
    private void scrollToBottom() {
        recyclerViewMessages.post(() -> {
            int messageCount = messageAdapter.getItemCount();
            if (messageCount > 0) {
                recyclerViewMessages.smoothScrollToPosition(messageCount - 1);
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ViewModel's onCleared will handle cleanup of resources
    }
}
