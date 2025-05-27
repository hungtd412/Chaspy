package com.example.chaspy.ui.view;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.lifecycle.ViewModelProvider;

import com.example.chaspy.R;
import com.example.chaspy.ui.adapter.MessageAdapter;
import com.example.chaspy.ui.viewmodel.ChatViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private CardView btnSend;
    private TextView txtUsername;
    private ImageView profileImage;

    private MessageAdapter messageAdapter;
    private LinearLayoutManager layoutManager;
    private ChatViewModel chatViewModel;

    private String conversationId;
    private String friendUsername;
    private String friendProfilePicUrl;
    private String friendId;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Get data from intent with null checks and defaults
        conversationId = getIntent().getStringExtra("conversationId");
        friendUsername = getIntent().getStringExtra("friendUsername");
        friendProfilePicUrl = getIntent().getStringExtra("friendProfilePicUrl");
        friendId = getIntent().getStringExtra("friendId");

        // Log the received data for debugging
        if (conversationId == null || friendUsername == null || friendId == null) {
            Toast.makeText(this, "Missing conversation information", Toast.LENGTH_SHORT).show();
            // Use defaults for testing or finish activity
            if (conversationId == null) conversationId = "default_conversation";
            if (friendUsername == null) friendUsername = "Unknown User";
            if (friendId == null) friendId = "unknown_user_id";
        }

        // Get current user id
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to chat", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = currentUser.getUid();

        // Initialize UI components
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        btnSend = findViewById(R.id.btnSend);
        txtUsername = findViewById(R.id.txtUsername);
        profileImage = findViewById(R.id.profileImage);

        // Setup RecyclerView
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Stack from bottom to show newest messages at bottom
        chatRecyclerView.setLayoutManager(layoutManager);

        // Create MessageAdapter with current user ID and friend's profile pic URL
        messageAdapter = new MessageAdapter(currentUserId, friendProfilePicUrl);
        chatRecyclerView.setAdapter(messageAdapter);

        // Setup UI with friend information
        txtUsername.setText(friendUsername);
        if (friendProfilePicUrl != null && !friendProfilePicUrl.isEmpty()) {
            Picasso.get()
                    .load(friendProfilePicUrl)
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .into(profileImage);
        } else {
            // Set default profile image
            profileImage.setImageResource(R.drawable.default_profile);
        }

        // Add a back button click listener
        CardView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Initialize ViewModel
        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        chatViewModel.init(conversationId, currentUserId);

        // Observe messages
        chatViewModel.getMessages().observe(this, messages -> {
            if (messages != null) {
                messageAdapter.setMessages(messages);
                scrollToBottom();
            }
        });

        // Observe new messages
        chatViewModel.getNewMessageAdded().observe(this, message -> {
            if (message != null) {
                messageAdapter.addMessage(message);
                scrollToBottom();
            }
        });

        // Observe errors
        chatViewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(ChatActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });

        // Add send button click listener
        btnSend.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                chatViewModel.sendMessage(message);
                messageInput.setText("");
            }
        });
    }

    private void scrollToBottom() {
        chatRecyclerView.post(() -> {
            int messageCount = messageAdapter.getItemCount();
            if (messageCount > 0) {
                chatRecyclerView.smoothScrollToPosition(messageCount - 1);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ViewModel's onCleared will handle cleanup of resources
    }
}