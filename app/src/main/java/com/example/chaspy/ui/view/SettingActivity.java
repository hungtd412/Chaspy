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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.chaspy.R;
import com.example.chaspy.data.manager.SharedPreferencesManager;
import com.example.chaspy.data.repository.UserRepository;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SettingActivity extends AppCompatActivity {

    private static final String TAG = "SettingActivity";
    private ImageView profileImage;
    private TextView txtUsername;
    private CardView editUsernameOption, editPasswordOption, friendsManagerOption, logoutOption;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private String currentUserId;

    private SharedPreferencesManager preferencesManager;
    private UserRepository userRepository; // Add this as a class field


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        // Initialize Firebase Auth and Database
        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            // Handle the case when user is not logged in
            Log.e(TAG, "User not logged in");
            finish();
            return;
        }

        userRepository = new UserRepository();

        currentUserId = currentUser.getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        profileImage = findViewById(R.id.profileImage);
        txtUsername = findViewById(R.id.txtUsername);
        editUsernameOption = findViewById(R.id.editUsernameOption);
        editPasswordOption = findViewById(R.id.editPasswordOption);
        friendsManagerOption = findViewById(R.id.friendsManagerOption);
        logoutOption = findViewById(R.id.logoutOption);
        CardView editProfileBtn = findViewById(R.id.editProfileBtn);

        preferencesManager = new SharedPreferencesManager(this);

        // Load user data
        loadUserData();

        // Set up button click listeners
        setupClickListeners();
        
        // Set up profile edit button
        editProfileBtn.setOnClickListener(v -> {
            Toast.makeText(SettingActivity.this, "Edit profile picture feature coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadUserData() {
        DatabaseReference userRef = databaseReference.child("users").child(currentUserId);
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Get user data
                    String firstName = snapshot.child("firstName").getValue(String.class);
                    String lastName = snapshot.child("lastName").getValue(String.class);
                    String profilePicUrl = snapshot.child("profilePicUrl").getValue(String.class);

                    // Display full name
                    String fullName = "";
                    if (firstName != null && lastName != null) {
                        fullName = firstName + " " + lastName;
                    } else if (firstName != null) {
                        fullName = firstName;
                    } else if (lastName != null) {
                        fullName = lastName;
                    } else {
                        fullName = "User";
                    }
                    
                    txtUsername.setText(fullName.toUpperCase());

                    // Load profile image
                    if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                        loadProfileImage(profilePicUrl);
                    } else {
                        // Load default avatar if no profile image is available
                        loadDefaultAvatar();
                    }
                } else {
                    Log.e(TAG, "User data not found in the database");
                    txtUsername.setText("USER");
                    loadDefaultAvatar();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
                txtUsername.setText("USER");
                loadDefaultAvatar();
            }
        });
    }

    private void loadProfileImage(String imageUrl) {
        Glide.with(this)
            .load(imageUrl)
            .apply(RequestOptions.circleCropTransform())
            .placeholder(R.drawable.background_parrot) // Using available drawable from XML
            .error(R.drawable.background_parrot)
            .into(profileImage);
    }

    private void loadDefaultAvatar() {
        // Get default avatar URL from general_information node
        DatabaseReference defaultAvatarRef = databaseReference.child("general_information").child("default_avatar");
        defaultAvatarRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String defaultAvatarUrl = snapshot.getValue(String.class);
                if (defaultAvatarUrl != null && !defaultAvatarUrl.isEmpty()) {
                    loadProfileImage(defaultAvatarUrl);
                } else {
                    // If default avatar URL is not found, use a local resource
                    profileImage.setImageResource(R.drawable.background_parrot);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
                // Use a local resource as fallback
                profileImage.setImageResource(R.drawable.background_parrot);
            }
        });
    }

    private void setupClickListeners() {
        editUsernameOption.setOnClickListener(v -> {
            // Show username edit popup
            showUsernameEditPopup();
        });

        editPasswordOption.setOnClickListener(v -> {
            // Show password edit popup
            showPasswordEditPopup();
        });

        friendsManagerOption.setOnClickListener(v -> {
            // Navigate to FriendsActivity
            Intent intent = new Intent(SettingActivity.this, FriendsActivity.class);
            startActivity(intent);
        });

        logoutOption.setOnClickListener(v -> {
            // Handle logout
            FirebaseAuth.getInstance().signOut();
            preferencesManager.setLoggedOut();
            Toast.makeText(SettingActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            // Navigate to login screen after logout
            Intent intent = new Intent(SettingActivity.this, SignInActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        CardView btnConversation = findViewById(R.id.btn_conversation);
        btnConversation.setOnClickListener(v -> {
            // Navigate back to ConversationActivity
            Intent intent = new Intent(SettingActivity.this, ConversationActivity.class);
            startActivity(intent);
        });
    }

    private void showUsernameEditPopup() {
        // Inflate the popup layout
        View popupView = getLayoutInflater().inflate(R.layout.popup_username, null);

        // Create the popup window with a fixed width matching the XML layout
        int width = (int) getResources().getDimension(R.dimen.popup_width);

        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true; // Allows taps outside the popup to dismiss it
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        // Set a background drawable with elevation for shadow effect
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setElevation(10);

        // Show the popup window centered in the screen
        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0);

        // Dim the background
        View rootView = getWindow().getDecorView().getRootView();
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) getWindow().getAttributes();
        params.alpha = 0.8f;
        getWindow().setAttributes(params);

        // Restore background alpha when popup is dismissed
        popupWindow.setOnDismissListener(() -> {
            params.alpha = 1f;
            getWindow().setAttributes(params);
        });


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

            // Update using UserRepository
            userRepository.updateUserProfile(currentUserId, newFirstName, newLastName)
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

        btnCancel.setOnClickListener(v -> popupWindow.dismiss());


        // Cancel button click handler
        btnCancel.setOnClickListener(v -> popupWindow.dismiss());

    }

    private void showPasswordEditPopup() {
        // Inflate the popup layout
        View popupView = getLayoutInflater().inflate(R.layout.popup_password, null);

        // Create the popup window with a fixed width matching the XML layout
        int width = (int) getResources().getDimension(R.dimen.popup_width);

        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        // Set a background drawable with elevation
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setElevation(10);

        // Show the popup window centered in the screen
        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0);

        // Dim the background
        View rootView = getWindow().getDecorView().getRootView();
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) getWindow().getAttributes();
        params.alpha = 0.8f;
        getWindow().setAttributes(params);

        // Restore background alpha when popup is dismissed
        popupWindow.setOnDismissListener(() -> {
            params.alpha = 1f;
            getWindow().setAttributes(params);
        });
        
        // Get references to views in the popup
        EditText currentPasswordInput = popupView.findViewById(R.id.currentPasswordInput);
        EditText newPasswordInput = popupView.findViewById(R.id.newPasswordInput);
        EditText confirmPasswordInput = popupView.findViewById(R.id.confirmPasswordInput);
        AppCompatButton btnChange = popupView.findViewById(R.id.btnChange);
        AppCompatButton btnCancel = popupView.findViewById(R.id.btnCancel);
        TextView forgotPasswordText = popupView.findViewById(R.id.forgotPasswordText);
        
        // Set up click listener for change button
        btnChange.setOnClickListener(v -> {
            String currentPassword = currentPasswordInput.getText().toString().trim();
            String newPassword = newPasswordInput.getText().toString().trim();
            String confirmPassword = confirmPasswordInput.getText().toString().trim();
            
            // Validate inputs
            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(SettingActivity.this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Check if new password meets Firebase requirements (min 6 characters)
            if (newPassword.length() < 6) {
                Toast.makeText(SettingActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Check if passwords match
            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(SettingActivity.this, "New passwords don't match", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Disable button to prevent multiple clicks
            btnChange.setEnabled(false);
            btnChange.setText("Updating...");
            
            // Use repository to change password
            userRepository.changePassword(currentPassword, newPassword)
                .addOnCompleteListener(task -> {
                    // Re-enable button
                    btnChange.setEnabled(true);
                    btnChange.setText("Change");
                    
                    if (task.isSuccessful()) {
                        Toast.makeText(SettingActivity.this, "Password updated successfully", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(SettingActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
        });
        
        // Set up click listener for forgot password link
        forgotPasswordText.setOnClickListener(v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                String email = user.getEmail();
                if (email != null && !email.isEmpty()) {
                    // Disable forgot password text temporarily
                    forgotPasswordText.setEnabled(false);
                    forgotPasswordText.setText("Sending email...");
                    
                    // Send password reset email
                    userRepository.sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            // Re-enable the text
                            forgotPasswordText.setEnabled(true);
                            forgotPasswordText.setText("Forgot password");
                            
                            if (task.isSuccessful()) {
                                Toast.makeText(SettingActivity.this, 
                                    "Password reset email sent to " + email, 
                                    Toast.LENGTH_SHORT).show();
                                popupWindow.dismiss();
                            } else {
                                Toast.makeText(SettingActivity.this, 
                                    "Failed to send reset email", 
                                    Toast.LENGTH_SHORT).show();
                            }
                        });
                }
            }
        });
        
        // Set up cancel button
        btnCancel.setOnClickListener(v -> popupWindow.dismiss());
    }
}
