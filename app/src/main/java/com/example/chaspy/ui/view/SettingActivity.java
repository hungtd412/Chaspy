package com.example.chaspy.ui.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.chaspy.R;
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
    private ImageView profileImageView;
    private TextView fullNameTextView;
    private MaterialButton editUsernameButton, editPasswordButton, friendManagersButton;
    
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
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
        profileImageView = findViewById(R.id.imageViewProfile);
        fullNameTextView = findViewById(R.id.textViewFullName);
        editUsernameButton = findViewById(R.id.buttonEditUsername);
        editPasswordButton = findViewById(R.id.buttonEditPassword);
        friendManagersButton = findViewById(R.id.buttonFriendManagers);
        
        // Load user data
        loadUserData();
        
        // Set up button click listeners
        setupClickListeners();
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
                    String fullName = firstName + " " + lastName;
                    fullNameTextView.setText(fullName);
                    
                    // Load profile image
                    if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                        loadProfileImage(profilePicUrl);
                    } else {
                        // Load default avatar if no profile image is available
                        loadDefaultAvatar();
                    }
                } else {
                    Log.e(TAG, "User data not found in the database");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
            }
        });
    }
    
    private void loadProfileImage(String imageUrl) {
        Glide.with(this)
            .load(imageUrl)
            .apply(RequestOptions.circleCropTransform())
            .placeholder(R.drawable.ic_person_placeholder) // Add a placeholder drawable resource
            .error(R.drawable.ic_person_placeholder) // Add an error drawable resource
            .into(profileImageView);
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
                    profileImageView.setImageResource(R.drawable.ic_person_placeholder); // Add a placeholder drawable resource
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
                // Use a local resource as fallback
                profileImageView.setImageResource(R.drawable.ic_person_placeholder); // Add a placeholder drawable resource
            }
        });
    }
    
    private void setupClickListeners() {
        editUsernameButton.setOnClickListener(v -> {
            // Handle edit username click
            // TODO: Implement username editing functionality
        });
        
        editPasswordButton.setOnClickListener(v -> {
            // Handle edit password click
            // TODO: Implement password changing functionality
        });
        
        friendManagersButton.setOnClickListener(v -> {
            // Navigate to FriendsActivity
            Intent intent = new Intent(SettingActivity.this, FriendsActivity.class);
            startActivity(intent);
        });
    }
}
