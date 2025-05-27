package com.example.chaspy.ui.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.chaspy.R;
import com.example.chaspy.data.manager.SharedPreferencesManager;
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
            // Handle edit username click
            Toast.makeText(SettingActivity.this, "Edit username feature coming soon", Toast.LENGTH_SHORT).show();
        });

        editPasswordOption.setOnClickListener(v -> {
            // Handle edit password click
            Toast.makeText(SettingActivity.this, "Edit password feature coming soon", Toast.LENGTH_SHORT).show();
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
            finish();
        });

        CardView btnConversation = findViewById(R.id.btn_conversation);
        btnConversation.setOnClickListener(v -> {
            // Navigate back to ConversationActivity
            Intent intent = new Intent(SettingActivity.this, ConversationActivity.class);
            startActivity(intent);
            finish(); // Finish current activity to avoid stacking
        });
    }
}
