package com.example.chaspy.ui.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;
import android.widget.Toast;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.app.ProgressDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.chaspy.R;
import com.example.chaspy.data.manager.SharedPreferencesManager;
import com.example.chaspy.data.model.User;
import com.example.chaspy.ui.viewmodel.SettingViewModel;
import com.google.firebase.auth.FirebaseUser;

public class SettingActivity extends AppCompatActivity {

    private static final String TAG = "SettingActivity";
    private ImageView profileImage;
    private TextView txtUsername;
    private CardView editProfileBtn, editUsernameOption, editPasswordOption, friendsManagerOption, logoutOption, btnConversation;
    private SharedPreferencesManager preferencesManager;
    private SettingViewModel viewModel;
    private ProgressDialog progressDialog;

    // Activity Result Launcher for gallery picker
    private ActivityResultLauncher<String> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(SettingViewModel.class);

        if (!viewModel.isUserLoggedIn()) {
            Log.e(TAG, "User not logged in");
            navigateToSignIn();
            return;
        }

        preferencesManager = new SharedPreferencesManager(this);

        initializeViews();

        // Initialize the gallery launcher
        registerGalleryLauncher();

        setupObservers();

        viewModel.loadUserData();

        setupClickListeners();
    }

    private void registerGalleryLauncher() {
        galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            result -> {
                if (result != null) {
                    // Show loading indicator
                    showLoading("Uploading image...");
                    
                    // Upload image to Cloudinary
                    viewModel.uploadProfileImage(this, result)
                        .addOnSuccessListener(imageUrl -> {
                            // Update UI with the new image
                            Glide.with(this)
                                .load(imageUrl)
                                .apply(RequestOptions.circleCropTransform())
                                .into(profileImage);
                            
                            hideLoading();
                            Toast.makeText(SettingActivity.this, "Profile picture updated successfully", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            hideLoading();
                            Toast.makeText(SettingActivity.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Image upload failed", e);
                        });
                }
            }
        );
    }

    private void showLoading(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }
    
    private void hideLoading() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void initializeViews() {
        profileImage = findViewById(R.id.profileImage);
        editProfileBtn = findViewById(R.id.editProfileBtn);
        txtUsername = findViewById(R.id.txtUsername);
        editUsernameOption = findViewById(R.id.editUsernameOption);
        editPasswordOption = findViewById(R.id.editPasswordOption);
        friendsManagerOption = findViewById(R.id.friendsManagerOption);
        logoutOption = findViewById(R.id.logoutOption);
        btnConversation = findViewById(R.id.btn_conversation);
    }

    private void setupObservers() {
        // Observe user data changes
        viewModel.getUserData().observe(this, this::updateUI);

        // Observe error messages
        viewModel.getErrorMessage().observe(this, message -> {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            Log.e(TAG, message);
        });
        
        // Observe loading state
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                showLoading("Processing...");
            } else {
                hideLoading();
            }
        });
    }

    private void updateUI(User user) {
        if (user != null) {
            // Update username
            txtUsername.setText(user.getFullName().toUpperCase());

            // Load profile image
            if (user.getProfilePicUrl() != null && !user.getProfilePicUrl().isEmpty()) {
                loadProfileImage(user.getProfilePicUrl());
            } else {
                // Load default avatar
                viewModel.getDefaultAvatarUrl().observe(this, url -> {
                    if (url != null && !url.isEmpty()) {
                        loadProfileImage(url);
                    } else {
                        profileImage.setImageResource(R.drawable.background_parrot);
                    }
                });
            }
        }
    }

    private void loadProfileImage(String imageUrl) {
        Glide.with(this)
            .load(imageUrl)
            .apply(RequestOptions.circleCropTransform())
            .placeholder(R.drawable.background_parrot)
            .error(R.drawable.background_parrot)
            .into(profileImage);
    }

    private void setupClickListeners() {
        editProfileBtn.setOnClickListener(v -> openGallery());

        profileImage.setOnClickListener(v -> showProfileImagePopup());

        editUsernameOption.setOnClickListener(v -> showUsernameEditPopup());

        editPasswordOption.setOnClickListener(v -> showPasswordEditPopup());

        friendsManagerOption.setOnClickListener(v -> {
            Intent intent = new Intent(SettingActivity.this, FriendsActivity.class);
            startActivity(intent);
        });

        logoutOption.setOnClickListener(v -> logout());

        btnConversation.setOnClickListener(v -> {
            Intent intent = new Intent(SettingActivity.this, ConversationActivity.class);
            startActivity(intent);
        });
    }

    private void openGallery() {
        galleryLauncher.launch("image/*");
    }

    private void showProfileImagePopup() {
        // Create popup view
        View popupView = getLayoutInflater().inflate(R.layout.popup_profile_image, null);
        PopupWindow popupWindow = createPopupWindow(popupView, R.dimen.popup_width);

        // Get references to views
        ImageView fullProfileImage = popupView.findViewById(R.id.fullProfileImage);

        // Get the current profile image URL from the ViewModel
        User currentUser = viewModel.getUserData().getValue();
        String imageUrl = (currentUser != null && currentUser.getProfilePicUrl() != null) ? 
            currentUser.getProfilePicUrl() : "";

        // Load profile image into popup
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.background_parrot)
            .error(R.drawable.background_parrot)
            .into(fullProfileImage);
    }

    private void showUsernameEditPopup() {
        // Create popup view
        View popupView = getLayoutInflater().inflate(R.layout.popup_username, null);
        PopupWindow popupWindow = createPopupWindow(popupView, R.dimen.popup_width);

        // Get references to views
        EditText firstNameInput = popupView.findViewById(R.id.firstNameInput);
        EditText lastNameInput = popupView.findViewById(R.id.lastNameInput);
        AppCompatButton btnSave = popupView.findViewById(R.id.btnSave);
        AppCompatButton btnCancel = popupView.findViewById(R.id.btnCancel);

        // Save button click handler
        btnSave.setOnClickListener(v -> {
            String newFirstName = firstNameInput.getText().toString().trim();
            String newLastName = lastNameInput.getText().toString().trim();

            if (newFirstName.isEmpty() || newLastName.isEmpty()) {
                Toast.makeText(SettingActivity.this, "Please fill in both names", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update profile using ViewModel
            viewModel.updateUserName(newFirstName, newLastName)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(SettingActivity.this, "Name updated successfully", Toast.LENGTH_SHORT).show();
                        txtUsername.setText((newFirstName + " " + newLastName).toUpperCase());
                        popupWindow.dismiss();
                    } else {
                        Toast.makeText(SettingActivity.this, "Failed to update name", Toast.LENGTH_SHORT).show();
                    }
                });
        });

        // Cancel button handler
        btnCancel.setOnClickListener(v -> popupWindow.dismiss());
    }

    private void showPasswordEditPopup() {
        // Create popup view
        View popupView = getLayoutInflater().inflate(R.layout.popup_password, null);
        PopupWindow popupWindow = createPopupWindow(popupView, R.dimen.popup_width);

        // Get references to views
        EditText currentPasswordInput = popupView.findViewById(R.id.currentPasswordInput);
        EditText newPasswordInput = popupView.findViewById(R.id.newPasswordInput);
        EditText confirmPasswordInput = popupView.findViewById(R.id.confirmPasswordInput);
        AppCompatButton btnChange = popupView.findViewById(R.id.btnChange);
        AppCompatButton btnCancel = popupView.findViewById(R.id.btnCancel);
        TextView forgotPasswordText = popupView.findViewById(R.id.forgotPasswordText);

        // Change password button handler
        btnChange.setOnClickListener(v -> handlePasswordChange(
            currentPasswordInput.getText().toString().trim(),
            newPasswordInput.getText().toString().trim(),
            confirmPasswordInput.getText().toString().trim(),
            btnChange,
            popupWindow
        ));

        // Forgot password handler
        forgotPasswordText.setOnClickListener(v -> handleForgotPassword(forgotPasswordText, popupWindow));

        // Cancel button handler
        btnCancel.setOnClickListener(v -> popupWindow.dismiss());
    }

    private void handlePasswordChange(String currentPassword, String newPassword, String confirmPassword, AppCompatButton btnChange, PopupWindow popupWindow) {
        // Validate inputs
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if new password meets Firebase requirements
        if (newPassword.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if passwords match
        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "New passwords don't match", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button to prevent multiple clicks
        btnChange.setEnabled(false);
        btnChange.setText("Updating...");

        // Use ViewModel to change password
        viewModel.changePassword(currentPassword, newPassword)
            .addOnCompleteListener(task -> {
                // Re-enable button
                btnChange.setEnabled(true);
                btnChange.setText("Change");

                if (task.isSuccessful()) {
                    Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                    popupWindow.dismiss();
                } else {
                    // Handle different error cases
                    String message = "Failed to update password";
                    if (task.getException() != null) {
                        String errorMessage = task.getException().getMessage();
                        if (errorMessage != null && errorMessage.contains("password is invalid")) {
                            message = "Current password is incorrect";
                        }
                    }
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void handleForgotPassword(TextView forgotPasswordText, PopupWindow popupWindow) {
        FirebaseUser user = viewModel.getCurrentUser();
        if (user != null) {
            String email = user.getEmail();
            if (email != null && !email.isEmpty()) {
                // Disable forgot password text temporarily
                forgotPasswordText.setEnabled(false);
                forgotPasswordText.setText("Sending email...");

                // Send password reset email
                viewModel.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        // Re-enable the text
                        forgotPasswordText.setEnabled(true);
                        forgotPasswordText.setText("Forgot password");

                        if (task.isSuccessful()) {
                            Toast.makeText(this,
                                "Password reset email sent to " + email,
                                Toast.LENGTH_SHORT).show();
                            popupWindow.dismiss();
                        } else {
                            Toast.makeText(this,
                                "Failed to send reset email",
                                Toast.LENGTH_SHORT).show();
                        }
                    });
            }
        }
    }

    private void logout() {
        viewModel.logout();
        preferencesManager.setLoggedOut();
        Toast.makeText(SettingActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        navigateToSignIn();
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

    private void navigateToSignIn() {
        Intent intent = new Intent(SettingActivity.this, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        hideLoading();
        super.onDestroy();
    }
}
