package com.example.chaspy.ui.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.AdapterView;
import android.widget.Spinner;
import java.util.Locale;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.lifecycle.ViewModelProvider;

import com.example.chaspy.R;
import com.example.chaspy.ui.adapter.CalendarAdapter;
import com.example.chaspy.ui.adapter.MessageAdapter;
import com.example.chaspy.ui.adapter.ScheduleMessageAdapter;
import com.example.chaspy.ui.viewmodel.ChatViewModel;
import com.example.chaspy.data.model.ScheduleMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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

    // Keep track of the currently selected color
    private View currentSelectedSelector = null;
    private String selectedColorHex = "#8CE4F0"; // Default color
    private PopupWindow sendLaterPopupWindow;
    private PopupWindow createSendLaterPopupWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Get data from intent with null checks and defaults
        conversationId = getIntent().getStringExtra("conversationId");
        friendUsername = getIntent().getStringExtra("friendUsername");
        friendProfilePicUrl = getIntent().getStringExtra("friendProfilePicUrl");
        friendId = getIntent().getStringExtra("friendId");

        // Get theme color from intent, use default if not provided
        String themeColor = getIntent().getStringExtra("themeColor");
        if (themeColor != null && !themeColor.isEmpty()) {
            selectedColorHex = themeColor;
        }

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

        // Apply theme color immediately
        applySelectedTheme();

        // Initialize popup button clicks
        setupPopupButtons();

        // Initialize ViewModel
        initializeViewModel();

        // Add friendId to ViewModel for scheduled messages
        if (friendId != null) {
            chatViewModel.setFriendId(friendId);
        }
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

        // Set up color selectors with current theme color
        setupColorSelectors(popupView);

        // Set click listeners
        btnChange.setOnClickListener(v -> {
            // Apply selected theme
            applySelectedTheme();

            // Update theme color in Firebase
            chatViewModel.updateThemeColor(conversationId, selectedColorHex);

            popupWindow.dismiss();
        });

        btnCancel.setOnClickListener(v -> popupWindow.dismiss());
    }

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

        // Hide all selectors initially
        selectorLightBlue.setVisibility(View.INVISIBLE);
        selectorBlue.setVisibility(View.INVISIBLE);
        selectorGreen.setVisibility(View.INVISIBLE);
        selectorPink.setVisibility(View.INVISIBLE);
        selectorCoral.setVisibility(View.INVISIBLE);
        selectorLavender.setVisibility(View.INVISIBLE);

        // Set initial selection based on current theme color
        View selectedSelector = null;
        switch (selectedColorHex) {
            case "#A9E7FD":
                selectedSelector = selectorLightBlue;
                break;
            case "#90CAF9":
                selectedSelector = selectorBlue;
                break;
            case "#A5D6A7":
                selectedSelector = selectorGreen;
                break;
            case "#F8BBD0":
                selectedSelector = selectorPink;
                break;
            case "#FF9E9E":
                selectedSelector = selectorCoral;
                break;
            case "#E1BEE7":
                selectedSelector = selectorLavender;
                break;
            default:
                // If color doesn't match any of the predefined colors, default to light blue
                selectedSelector = selectorLightBlue;
                selectedColorHex = "#A9E7FD";
                break;
        }
        selectedSelector.setVisibility(View.VISIBLE);
        currentSelectedSelector = selectedSelector;

        // Set click listeners for each color
        colorLightBlue.setOnClickListener(v -> {
            updateColorSelection(selectorLightBlue);
            selectedColorHex = "#A9E7FD";
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
        // Update the UI colors for header and bottom layouts
        View headerLayout = findViewById(R.id.headerLayout);
        View bottomLayout = findViewById(R.id.bottomLayout);

        // Try to set background color directly
        if (headerLayout != null && bottomLayout != null) {
            int color = Color.parseColor(selectedColorHex);
            headerLayout.setBackgroundColor(color);
            bottomLayout.setBackgroundColor(color);
        }

        // Update theme color in the adapter and refresh all messages
        if (messageAdapter != null) {
            messageAdapter.setThemeColor(selectedColorHex);
            messageAdapter.notifyDataSetChanged();
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
        View sendLaterLayout = popupView.findViewById(R.id.sendLaterButtonLayout);

        cameraLayout.setOnClickListener(v -> {
            // Handle camera action
            Toast.makeText(ChatActivity.this, "Camera clicked", Toast.LENGTH_SHORT).show();
            plusPopupWindow.dismiss();
        });

        sendLaterLayout.setOnClickListener(v -> showSendLaterPopup());

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

    private PopupWindow createPopupWindow(View popupView, int widthDimensionId) {
        // Create popup window with fixed width
        int width = (int) getResources().getDimension(widthDimensionId);
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
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

        return popupWindow;
    }

    private void showSendLaterPopup() {
        View popupViewSendLater = getLayoutInflater().inflate(R.layout.popup_send_later, null);
        sendLaterPopupWindow = createPopupWindow(popupViewSendLater, R.dimen.popup_width_medium);

        // Initialize RecyclerView
        RecyclerView recyclerView = popupViewSendLater.findViewById(R.id.recyclerViewScheduled);
        TextView emptyStateText = popupViewSendLater.findViewById(R.id.emptyStateText);

        // Initialize adapter
        ScheduleMessageAdapter adapter = new ScheduleMessageAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Set up delete click listener
        adapter.setOnItemClickListener(new ScheduleMessageAdapter.OnItemClickListener() {
            @Override
            public void onDeleteClick(ScheduleMessage message) {
                showDeleteConfirmationDialog(message);
            }
        });

        // Set click listeners
        ImageView btnAdd = popupViewSendLater.findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(v -> addSendLaterItem());

        // Observe scheduled messages from ViewModel
        chatViewModel.getScheduledMessages().observe(this, messages -> {
            if (messages != null && !messages.isEmpty()) {
                adapter.setScheduledMessages(messages);
                recyclerView.setVisibility(View.VISIBLE);
                emptyStateText.setVisibility(View.GONE);
            } else {
                recyclerView.setVisibility(View.GONE);
                emptyStateText.setVisibility(View.VISIBLE);
            }
        });

        // Observe loading state
        chatViewModel.getIsLoading().observe(this, isLoading -> {
            // You can add loading indicator if needed
        });

        // Load scheduled messages
        chatViewModel.loadScheduledMessages();

        // Dismiss the plus popup if showing
        if (plusPopupWindow != null && plusPopupWindow.isShowing()) {
            plusPopupWindow.dismiss();
        }
    }

    private void showDeleteConfirmationDialog(ScheduleMessage message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Scheduled Message");
        builder.setMessage("Are you sure you want to delete this scheduled message?");

        builder.setPositiveButton("Delete", (dialog, which) -> {
            // Call ViewModel to delete the message
            if (message.getId() != null) {
                chatViewModel.deleteScheduledMessage(message.getId());
                Toast.makeText(ChatActivity.this, "Message deleted", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            // Get the selected date from the calendar
            long dateMillis = data.getLongExtra(CalendarActivity.EXTRA_SELECTED_DATE, -1);
            if (dateMillis != -1) {
                Calendar selectedDate = Calendar.getInstance();
                selectedDate.setTimeInMillis(dateMillis);

                // Update the time result text view in the popup
                if (createSendLaterPopupWindow != null && createSendLaterPopupWindow.isShowing()) {
                    View popupView = createSendLaterPopupWindow.getContentView();
                    TextView tvTimeResult = popupView.findViewById(R.id.tvDateResult);

                    // Format the date for display
                    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault());
                    String formattedDate = dateFormat.format(selectedDate.getTime());

                    tvTimeResult.setText(formattedDate);
                }
            }
        }
    }

    private void addSendLaterItem() {
        // Before showing the new popup, make sure to dismiss the first one
        if (sendLaterPopupWindow != null && sendLaterPopupWindow.isShowing()) {
            sendLaterPopupWindow.dismiss();
        }

        // Inflate the create send later popup
        View popupCreateSendLater = getLayoutInflater().inflate(R.layout.popup_create_send_later, null);

        // Create popup window using the helper method
        createSendLaterPopupWindow = createPopupWindow(popupCreateSendLater, R.dimen.popup_width);

        // Get references to time-related views
        EditText etMessage = popupCreateSendLater.findViewById(R.id.etMessage);
        EditText etHour = popupCreateSendLater.findViewById(R.id.etHour);
        EditText etMinute = popupCreateSendLater.findViewById(R.id.etMinute);
        ImageButton btnIncreaseHour = popupCreateSendLater.findViewById(R.id.btnIncreaseHour);
        ImageButton btnDecreaseHour = popupCreateSendLater.findViewById(R.id.btnDecreaseHour);
        ImageButton btnIncreaseMinute = popupCreateSendLater.findViewById(R.id.btnIncreaseMinute);
        ImageButton btnDecreaseMinute = popupCreateSendLater.findViewById(R.id.btnDecreaseMinute);
        TextView tvTimeResult = popupCreateSendLater.findViewById(R.id.tvTimeResult);
        TextView tvDateResult = popupCreateSendLater.findViewById(R.id.tvDateResult);
        Spinner spinnerAmPm = popupCreateSendLater.findViewById(R.id.spinnerAmPm);
        ImageButton btnCalendar = popupCreateSendLater.findViewById(R.id.btnCalendar);
        AppCompatButton btnCreate = popupCreateSendLater.findViewById(R.id.btnCreate);
        AppCompatButton btnCancel = popupCreateSendLater.findViewById(R.id.btnCancel);

        // Initialize with current time
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR);
        if (currentHour == 0) currentHour = 12; // Convert 0 to 12 for 12-hour format
        int currentMinute = calendar.get(Calendar.MINUTE);
        int amPm = calendar.get(Calendar.AM_PM);

        // Set initial values
        etHour.setText(String.valueOf(currentHour));
        etMinute.setText(String.format(Locale.getDefault(), "%02d", currentMinute));
        spinnerAmPm.setSelection(amPm); // 0 for AM, 1 for PM

        // Update time result display
        updateTimeDisplay(tvTimeResult, etHour, etMinute, spinnerAmPm);

        // Set click listeners for hour buttons
        btnIncreaseHour.setOnClickListener(v -> {
            int hour = parseIntSafely(etHour.getText().toString(), 12);
            hour = (hour % 12) + 1; // Cycle from 1 to 12
            etHour.setText(String.valueOf(hour));
            updateTimeDisplay(tvTimeResult, etHour, etMinute, spinnerAmPm);
        });

        btnDecreaseHour.setOnClickListener(v -> {
            int hour = parseIntSafely(etHour.getText().toString(), 12);
            hour = (hour - 2 + 12) % 12 + 1; // Cycle from 12 to 1
            etHour.setText(String.valueOf(hour));
            updateTimeDisplay(tvTimeResult, etHour, etMinute, spinnerAmPm);
        });

        // Set click listeners for minute buttons
        btnIncreaseMinute.setOnClickListener(v -> {
            int minute = parseIntSafely(etMinute.getText().toString(), 0);
            minute = (minute + 1) % 60;
            etMinute.setText(String.format(Locale.getDefault(), "%02d", minute));
            updateTimeDisplay(tvTimeResult, etHour, etMinute, spinnerAmPm);
        });

        btnDecreaseMinute.setOnClickListener(v -> {
            int minute = parseIntSafely(etMinute.getText().toString(), 0);
            minute = (minute - 1 + 60) % 60;
            etMinute.setText(String.format(Locale.getDefault(), "%02d", minute));
            updateTimeDisplay(tvTimeResult, etHour, etMinute, spinnerAmPm);
        });

        // Set listener for AM/PM spinner
        spinnerAmPm.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateTimeDisplay(tvTimeResult, etHour, etMinute, spinnerAmPm);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Add text change listeners to update time display and validate input
        etHour.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    int hour = parseIntSafely(s.toString(), 12);
                    if (hour < 1) hour = 1;
                    if (hour > 12) hour = 12;
                    if (Integer.parseInt(s.toString()) != hour) {
                        etHour.setText(String.valueOf(hour));
                        etHour.setSelection(etHour.getText().length());
                    }
                }
                updateTimeDisplay(tvTimeResult, etHour, etMinute, spinnerAmPm);
            }
        });

        // For the minute input field
        etMinute.addTextChangedListener(new TextWatcher() {
            private boolean isUpdating = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating) return;

                isUpdating = true;

                String text = s.toString();
                if (text.isEmpty()) {
                    // When empty, don't auto-format yet to allow typing
                    isUpdating = false;
                    return;
                }

                try {
                    // Parse the input as integer
                    int minute = Integer.parseInt(text);

                    // Handle valid range
                    if (minute > 59) {
                        etMinute.setText("59");
                        etMinute.setSelection(2); // Place cursor at end
                    } else if (text.length() == 2) {
                        // Already has 2 digits, no formatting needed
                        // Just ensure it's within range
                        if (minute < 0) {
                            etMinute.setText("00");
                            etMinute.setSelection(2);
                        }
                    } else if (text.length() == 1) {
                        // Single digit - keep as is while editing
                        if (minute < 0) {
                            etMinute.setText("0");
                            etMinute.setSelection(1);
                        }
                    }
                } catch (NumberFormatException e) {
                    // If not a valid number, reset to 00
                    etMinute.setText("00");
                    etMinute.setSelection(2);
                }

                updateTimeDisplay(tvTimeResult, etHour, etMinute, spinnerAmPm);
                isUpdating = false;
            }
        });

        // Add focus listener to format minutes properly when focus changes
        etMinute.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                // When losing focus, ensure proper format with leading zeros
                String text = etMinute.getText().toString();
                if (text.isEmpty()) {
                    etMinute.setText("00");
                } else {
                    try {
                        int minute = Integer.parseInt(text);
                        if (minute < 10) {
                            etMinute.setText(String.format(Locale.getDefault(), "%02d", minute));
                        }
                    } catch (NumberFormatException e) {
                        etMinute.setText("00");
                    }
                }
                updateTimeDisplay(tvTimeResult, etHour, etMinute, spinnerAmPm);
            }
        });

        // Set up calendar button
        btnCalendar.setOnClickListener(v -> {
            showCalendarDialog();
        });

        // Set click listener for cancel button
        btnCancel.setOnClickListener(v -> {
            createSendLaterPopupWindow.dismiss();
            // Show the send later popup again
            showSendLaterPopup();
        });

        // Set click listener for create button
        btnCreate.setOnClickListener(v -> {
            String message = etMessage.getText().toString().trim();
            String hour = etHour.getText().toString().trim();
            String minute = etMinute.getText().toString().trim();
            String timeStr = tvTimeResult.getText().toString().trim();
            String dateStr = tvDateResult.getText().toString().trim();

            if (message.isEmpty()) {
                Toast.makeText(ChatActivity.this, "Please enter a message", Toast.LENGTH_SHORT).show();
                return;
            }

            if (hour.isEmpty() || minute.isEmpty() || "Time here".equals(timeStr)) {
                Toast.makeText(ChatActivity.this, "Please set a valid time", Toast.LENGTH_SHORT).show();
                return;
            }

            if ("Date here".equals(dateStr)) {
                Toast.makeText(ChatActivity.this, "Please select a date", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if the selected date and time are in the past
            try {
                // Parse the date
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault());
                Date selectedDate = dateFormat.parse(dateStr);

                // Parse the time
                int hourValue = Integer.parseInt(hour);
                int minuteValue = Integer.parseInt(minute);
                boolean isPM = spinnerAmPm.getSelectedItem().toString().equals("PM");
                
                // Convert 12-hour format to 24-hour format
                int hour24Format;
                if (isPM) {
                    // PM: Add 12 to hours, except for 12 PM which stays as 12
                    hour24Format = (hourValue == 12) ? 12 : hourValue + 12;
                } else {
                    // AM: Keep hours as is, except for 12 AM which becomes 0
                    hour24Format = (hourValue == 12) ? 0 : hourValue;
                }
                
                // Log the time conversion for debugging
                Log.d("ChatActivity", String.format("Time conversion: %d:%02d %s → %02d:%02d (24-hour)",
                        hourValue, minuteValue, isPM ? "PM" : "AM", hour24Format, minuteValue));

                // Create calendar with selected date and time (using 24-hour format)
                Calendar scheduledTime = Calendar.getInstance();
                scheduledTime.setTime(selectedDate);
                scheduledTime.set(Calendar.HOUR_OF_DAY, hour24Format);  // Use 24-hour format
                scheduledTime.set(Calendar.MINUTE, minuteValue);
                scheduledTime.set(Calendar.SECOND, 0);
                scheduledTime.set(Calendar.MILLISECOND, 0);
                // No need to set AM_PM when using HOUR_OF_DAY

                // Compare with current time
                Calendar now = Calendar.getInstance();
                if (scheduledTime.before(now)) {
                    Toast.makeText(ChatActivity.this, "Cannot schedule messages in the past", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Get timestamp (milliseconds since epoch)
                long scheduledTimestamp = scheduledTime.getTimeInMillis();
                
                // Log the scheduled time for verification
                SimpleDateFormat logFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Log.d("ChatActivity", "Scheduling message for: " + logFormat.format(new Date(scheduledTimestamp)) + 
                      " in conversation: " + conversationId);

                // Create and save the scheduled message
                chatViewModel.addScheduledMessage(message, scheduledTimestamp);
                
                Toast.makeText(ChatActivity.this, "Message scheduled successfully", Toast.LENGTH_SHORT).show();
                createSendLaterPopupWindow.dismiss();
                
                // Show the send later popup again to see the updated list
                showSendLaterPopup();

            } catch (Exception e) {
                Toast.makeText(ChatActivity.this, "Invalid date or time format", Toast.LENGTH_SHORT).show();
                Log.e("ChatActivity", "Error scheduling message: " + e.getMessage(), e);
            }
        });
    }

    private void updateTimeDisplay(TextView tvTimeResult, EditText etHour, EditText etMinute, android.widget.Spinner spinnerAmPm) {
        String hour = etHour.getText().toString();
        String minute = etMinute.getText().toString();
        String amPm = spinnerAmPm.getSelectedItem().toString();

        if (hour.isEmpty()) hour = "12";
        if (minute.isEmpty()) minute = "00";

        String timeString = hour + ":" + minute + " " + amPm;
        tvTimeResult.setText(timeString);
    }

    private int parseIntSafely(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void showCalendarDialog() {
        // Create calendar dialog using AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View calendarView = getLayoutInflater().inflate(R.layout.date_picker_dialog, null);
        builder.setView(calendarView);

        AlertDialog calendarDialog = builder.create();
        calendarDialog.show();

        // Get all required views from the dialog layout
        GridView calendarGridView = calendarView.findViewById(R.id.calendarGridView);
        TextView tvYear = calendarView.findViewById(R.id.tvYear);
        TextView tvSelectedDate = calendarView.findViewById(R.id.tvSelectedDate);
        TextView tvCurrentMonth = calendarView.findViewById(R.id.tvCurrentMonth);
        ImageButton btnPreviousMonth = calendarView.findViewById(R.id.btnPreviousMonth);
        ImageButton btnNextMonth = calendarView.findViewById(R.id.btnNextMonth);
        Button btnCancel = calendarView.findViewById(R.id.btnCancel);
        Button btnOk = calendarView.findViewById(R.id.btnOk);

        // Initialize today's date for comparison
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        // Initialize the calendar with today's date
        Calendar selectedDate = Calendar.getInstance();
        CalendarAdapter calendarAdapter = new CalendarAdapter(this, selectedDate);
        calendarGridView.setAdapter(calendarAdapter);

        // Pre-select today's date in the calendar
        int todayPosition = calendarAdapter.getTodayPosition();
        if (todayPosition >= 0) {
            calendarAdapter.setSelectedDate(todayPosition);
            calendarGridView.setSelection(todayPosition);
        }

        // Update header texts
        SimpleDateFormat headerDateFormat = new SimpleDateFormat("EEE, d MMM", Locale.getDefault());
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

        tvYear.setText(yearFormat.format(selectedDate.getTime()));
        tvCurrentMonth.setText(monthYearFormat.format(calendarAdapter.getCurrentCalendar().getTime()));
        tvSelectedDate.setText(headerDateFormat.format(selectedDate.getTime()));

        // Set up listeners
        calendarGridView.setOnItemClickListener((parent, view, position, id) -> {
            Date date = (Date) calendarAdapter.getItem(position);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            // Reset time part for proper comparison
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            // Check if date is in the past
            if (cal.before(today)) {
                Toast.makeText(ChatActivity.this, "Cannot select dates in the past", Toast.LENGTH_SHORT).show();
                return;
            }

            // If clicked on a day from prev/next month, switch to that month
            if (cal.get(Calendar.MONTH) != calendarAdapter.getCurrentCalendar().get(Calendar.MONTH)) {
                calendarAdapter.setCurrentMonth(cal);
                tvCurrentMonth.setText(monthYearFormat.format(calendarAdapter.getCurrentCalendar().getTime()));
            }

            selectedDate.setTime(date);
            calendarAdapter.setSelectedDate(position);
            tvYear.setText(yearFormat.format(selectedDate.getTime()));
            tvSelectedDate.setText(headerDateFormat.format(selectedDate.getTime()));
        });

        btnPreviousMonth.setOnClickListener(v -> {
            // Check if previous month is before current month
            Calendar prevMonth = (Calendar) calendarAdapter.getCurrentCalendar().clone();
            prevMonth.add(Calendar.MONTH, -1);

            // Allow going to previous month only if it's not before current month
            if (prevMonth.get(Calendar.YEAR) > today.get(Calendar.YEAR) ||
                    (prevMonth.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                            prevMonth.get(Calendar.MONTH) >= today.get(Calendar.MONTH))) {
                calendarAdapter.previousMonth();
                tvCurrentMonth.setText(monthYearFormat.format(calendarAdapter.getCurrentCalendar().getTime()));
            } else {
                Toast.makeText(ChatActivity.this, "Cannot navigate to past months", Toast.LENGTH_SHORT).show();
            }
        });

        btnNextMonth.setOnClickListener(v -> {
            calendarAdapter.nextMonth();
            tvCurrentMonth.setText(monthYearFormat.format(calendarAdapter.getCurrentCalendar().getTime()));
        });

        btnCancel.setOnClickListener(v -> calendarDialog.dismiss());

        btnOk.setOnClickListener(v -> {
            // Use the selected date (which defaults to today if none was selected)
            // Format the date for display
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault());
            String formattedDate = dateFormat.format(selectedDate.getTime());

            // Update the date result text view in the popup
            if (createSendLaterPopupWindow != null && createSendLaterPopupWindow.isShowing()) {
                View popupView = createSendLaterPopupWindow.getContentView();
                TextView tvDateResult = popupView.findViewById(R.id.tvDateResult);
                tvDateResult.setText(formattedDate);
            }

            calendarDialog.dismiss();
        });
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
        if (sendLaterPopupWindow != null && sendLaterPopupWindow.isShowing()) {
            sendLaterPopupWindow.dismiss();
        }
        if (createSendLaterPopupWindow != null && createSendLaterPopupWindow.isShowing()) {
            createSendLaterPopupWindow.dismiss();
        }
    }
}
