package com.example.chaspy.ui.view;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

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
    private PopupWindow plusPopupWindow;
    private PopupWindow menuPopupWindow;
    private ImageView btnMenu;
    private CardView btnAdd;

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
        initializeViews();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup UI with friend information
        setupUserInterface();

        // Initialize popup button clicks
        setupPopupButtons();

        // Initialize ViewModel
        initializeViewModel();
    }

    private void initializeViews() {
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        btnSend = findViewById(R.id.btnSend);
        txtUsername = findViewById(R.id.txtUsername);
        profileImage = findViewById(R.id.profileImage);
        btnMenu = findViewById(R.id.btnMenu);
        btnAdd = findViewById(R.id.btnAdd);

        // Add a back button click listener
        CardView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Add send button click listener
        btnSend.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                chatViewModel.sendMessage(message);
                messageInput.setText("");
            }
        });
    }

    private void setupRecyclerView() {
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Stack from bottom to show newest messages at bottom
        chatRecyclerView.setLayoutManager(layoutManager);

        // Create MessageAdapter with current user ID and friend's profile pic URL
        messageAdapter = new MessageAdapter(currentUserId, friendProfilePicUrl);
        chatRecyclerView.setAdapter(messageAdapter);
    }

    private void setupUserInterface() {
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
    }

    private void setupPopupButtons() {
        // Set click listeners for popup buttons
        btnMenu.setOnClickListener(v -> showMenuPopup(v));
        btnAdd.setOnClickListener(v -> showPlusPopup(v));
    }

    private void initializeViewModel() {
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
    }

    private void scrollToBottom() {
        chatRecyclerView.post(() -> {
            int messageCount = messageAdapter.getItemCount();
            if (messageCount > 0) {
                chatRecyclerView.smoothScrollToPosition(messageCount - 1);
            }
        });
    }

    private void showMenuPopup(View anchorView) {
        // Inflate the popup layout
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_name_theme, null);

        // Create the popup window
        menuPopupWindow = new PopupWindow(
                popupView,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                true
        );

        // Set animations and elevation
        menuPopupWindow.setElevation(20);
        menuPopupWindow.setAnimationStyle(android.R.style.Animation_Dialog);

        // Set click listeners for popup items
        View nicknameLayout = popupView.findViewById(R.id.nicknameLayout);
        View themeLayout = popupView.findViewById(R.id.themeLayout);

        nicknameLayout.setOnClickListener(v -> {
            // First dismiss the menu popup
            menuPopupWindow.dismiss();
            // Then show the nickname popup
            showNicknamePopup();
        });

        themeLayout.setOnClickListener(v -> {
            // First dismiss the menu popup
            menuPopupWindow.dismiss();
            // Then show the theme popup
            showThemePopup();
        });

        // Show the popup window
        // The location offset is relative to the anchor view
        menuPopupWindow.showAsDropDown(anchorView, -150, 0, Gravity.END);
    }

    private void showNicknamePopup() {
        // Inflate the popup layout
        View popupView = getLayoutInflater().inflate(R.layout.popup_nickname, null);

        // Create the popup window with fixed width from XML
        int width = (int) getResources().getDimension(R.dimen.popup_width);
        int height = WindowManager.LayoutParams.WRAP_CONTENT;
        boolean focusable = true;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        // Set background and elevation
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setElevation(10);

        // Show popup centered in screen
        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0);

        // Dim the background
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.alpha = 0.8f;
        getWindow().setAttributes(params);

        // Restore background alpha when popup is dismissed
        popupWindow.setOnDismissListener(() -> {
            params.alpha = 1f;
            getWindow().setAttributes(params);
        });

        // Get references to views
        EditText editNickname = popupView.findViewById(R.id.editNickname);
        Button btnChangeNickname = popupView.findViewById(R.id.btnChangeNickname);
        Button btnCancel = popupView.findViewById(R.id.btnCancel);

        // Set current nickname if available
        editNickname.setText(friendUsername);

        // Set click listeners
        btnChangeNickname.setOnClickListener(v -> {
            String newNickname = editNickname.getText().toString().trim();
            if (!newNickname.isEmpty()) {
                // Update nickname in database
                // chatViewModel.updateFriendNickname(friendId, newNickname);

                // Update UI
                txtUsername.setText(newNickname);
                friendUsername = newNickname;
                Toast.makeText(this, "Nickname updated", Toast.LENGTH_SHORT).show();
            }
            popupWindow.dismiss();
        });

        btnCancel.setOnClickListener(v -> popupWindow.dismiss());
    }

    private void showThemePopup() {
        // Inflate the popup layout
        View popupView = getLayoutInflater().inflate(R.layout.popup_theme, null);

        // Create the popup window
        int width = (int) getResources().getDimension(R.dimen.popup_width);
        int height = WindowManager.LayoutParams.WRAP_CONTENT;
        boolean focusable = true;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        // Set background and elevation
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setElevation(10);

        // Show popup centered in screen
        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0);

        // Dim the background
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.alpha = 0.8f;
        getWindow().setAttributes(params);

        // Restore background alpha when popup is dismissed
        popupWindow.setOnDismissListener(() -> {
            params.alpha = 1f;
            getWindow().setAttributes(params);
        });

        // Get references to views
        Button btnChange = popupView.findViewById(R.id.btnChange);
        Button btnCancel = popupView.findViewById(R.id.btnCancel);

        // Set up color selectors
        setupColorSelectors(popupView);

        // Set click listeners
        btnChange.setOnClickListener(v -> {
            // Apply selected theme
            applySelectedTheme();
            popupWindow.dismiss();
        });

        btnCancel.setOnClickListener(v -> popupWindow.dismiss());
    }

    // Keep track of the currently selected color
    private View currentSelectedSelector = null;
    private String selectedColorHex = "#8CE4F0"; // Default color

    private void setupColorSelectors(View popupView) {
        // Get all color views
        View colorLightBlue = popupView.findViewById(R.id.colorLightBlue);
        View colorBlue = popupView.findViewById(R.id.colorBlue);
        View colorGreen = popupView.findViewById(R.id.colorGreen);
        View colorPink = popupView.findViewById(R.id.colorPink);
        View colorCoral = popupView.findViewById(R.id.colorCoral);
        View colorLavender = popupView.findViewById(R.id.colorLavender);

        // Get all selector views
        View selectorLightBlue = popupView.findViewById(R.id.selectorLightBlue);
        View selectorBlue = popupView.findViewById(R.id.selectorBlue);
        View selectorGreen = popupView.findViewById(R.id.selectorGreen);
        View selectorPink = popupView.findViewById(R.id.selectorPink);
        View selectorCoral = popupView.findViewById(R.id.selectorCoral);
        View selectorLavender = popupView.findViewById(R.id.selectorLavender);

        // Default selection
        selectorLightBlue.setVisibility(View.VISIBLE);
        currentSelectedSelector = selectorLightBlue;

        // Set click listeners for each color
        colorLightBlue.setOnClickListener(v -> {
            updateColorSelection(selectorLightBlue);
            selectedColorHex = "#CCEEFF";
        });

        colorBlue.setOnClickListener(v -> {
            updateColorSelection(selectorBlue);
            selectedColorHex = "#90CAF9";
        });

        colorGreen.setOnClickListener(v -> {
            updateColorSelection(selectorGreen);
            selectedColorHex = "#A5D6A7";
        });

        colorPink.setOnClickListener(v -> {
            updateColorSelection(selectorPink);
            selectedColorHex = "#F8BBD0";
        });

        colorCoral.setOnClickListener(v -> {
            updateColorSelection(selectorCoral);
            selectedColorHex = "#FF9E9E";
        });

        colorLavender.setOnClickListener(v -> {
            updateColorSelection(selectorLavender);
            selectedColorHex = "#E1BEE7";
        });
    }

    private void updateColorSelection(View newSelector) {
        // Hide previous selection
        if (currentSelectedSelector != null) {
            currentSelectedSelector.setVisibility(View.INVISIBLE);
        }

        // Show new selection
        newSelector.setVisibility(View.VISIBLE);
        currentSelectedSelector = newSelector;
    }

    private void applySelectedTheme() {
        // Here you would update the theme in the database
        // chatViewModel.updateConversationTheme(conversationId, selectedColorHex);

        // For now, just update the UI
        CardView headerCard = findViewById(R.id.headerCard);
        CardView bottomCard = findViewById(R.id.bottomCard);

        if (headerCard != null && bottomCard != null) {
            int color = android.graphics.Color.parseColor(selectedColorHex);
            headerCard.setCardBackgroundColor(color);
            bottomCard.setCardBackgroundColor(color);

            Toast.makeText(this, "Theme updated", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPlusPopup(View anchorView) {
        // Inflate the popup layout
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_plus, null);

        // Create the popup window
        plusPopupWindow = new PopupWindow(
                popupView,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                true
        );

        // Set animations and elevation
        plusPopupWindow.setElevation(20);
        plusPopupWindow.setAnimationStyle(android.R.style.Animation_Dialog);

        // Set click listeners for popup items
        View cameraLayout = popupView.findViewById(R.id.cameraLayout);
        View locationLayout = popupView.findViewById(R.id.locationLayout);

        cameraLayout.setOnClickListener(v -> {
            // Handle camera action
            Toast.makeText(ChatActivity.this, "Camera clicked", Toast.LENGTH_SHORT).show();
            plusPopupWindow.dismiss();
        });

        locationLayout.setOnClickListener(v -> {
            // Handle location action
            Toast.makeText(ChatActivity.this, "Send location clicked", Toast.LENGTH_SHORT).show();
            plusPopupWindow.dismiss();
        });

        // Calculate position - this fixes the position issue for the plus popup
        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);

        // Show the popup window
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupHeight = popupView.getMeasuredHeight();

        // Position above the button
        plusPopupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY,
                location[0], location[1] - popupHeight);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Dismiss any open popups to prevent window leaks
        if (plusPopupWindow != null && plusPopupWindow.isShowing()) {
            plusPopupWindow.dismiss();
        }
        if (menuPopupWindow != null && menuPopupWindow.isShowing()) {
            menuPopupWindow.dismiss();
        }
    }
}