package com.example.chaspy.data.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthResult;
import com.google.android.gms.tasks.Task;
import com.example.chaspy.data.model.User;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.android.gms.tasks.TaskCompletionSource;

import java.util.HashMap;
import java.util.Map;

public class UserFirebaseService {

    private static FirebaseAuth firebaseAuth;
    private static DatabaseReference usersRef;
    private static String defaultAvatarUrl = ""; // Cache the default avatar URL

    public UserFirebaseService() {
        firebaseAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        // Initialize default avatar URL
        fetchDefaultAvatarUrl();
    }

    public Task<AuthResult> registerUser(String email, String password) {
        return firebaseAuth.createUserWithEmailAndPassword(email, password);
    }

    public Task<Void> sendEmailVerification(FirebaseUser user) {
        return user.sendEmailVerification();
    }
    
    public boolean isEmailVerified(FirebaseUser user) {
        if (user != null) {
            user.reload();
            return user.isEmailVerified();
        }
        return false;
    }

    public Task<AuthResult> signInUser(String email, String password) {
        return firebaseAuth.signInWithEmailAndPassword(email, password);
    }

    // Sign out the current user
    public void signOut() {
        firebaseAuth.signOut();
    }
    
    // Resend verification email
    public Task<Void> resendVerificationEmail(String email, String password) {
        // Sign in with provided credentials first to access the user object
        return firebaseAuth.signInWithEmailAndPassword(email, password)
            .continueWithTask(signInTask -> {
                if (signInTask.isSuccessful()) {
                    FirebaseUser user = signInTask.getResult().getUser();
                    return user.sendEmailVerification().continueWithTask(task -> {
                        // Sign out after sending verification email
                        firebaseAuth.signOut();
                        return task;
                    });
                } else {
                    // Handle the failed sign-in
                    TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
                    taskCompletionSource.setException(signInTask.getException());
                    return taskCompletionSource.getTask();
                }
            });
    }

    // Save additional user data in Firestore
    public Task<Void> saveUserData(FirebaseUser firebaseUser, String firstName, String lastName) {
        // Get default avatar URL asynchronously
        return getDefaultProfilePicUrl().continueWithTask(task -> {
            if (task.isSuccessful()) {
                String avatarUrl = task.getResult();
                User user = new User(firebaseUser.getEmail(), firstName, lastName, avatarUrl, false);
                System.out.println(user);
                return usersRef
                        .child(firebaseUser.getUid())
                        .setValue(user);
            } else {
                throw task.getException();
            }
        });
    }

    /**
     * Update user profile data (first name and last name)
     * @param userId The user ID to update
     * @param firstName New first name
     * @param lastName New last name
     * @return Task result of the update operation
     */
    public Task<Void> updateUserProfile(String userId, String firstName, String lastName) {
        // Create a map of the fields to update
        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", firstName);
        updates.put("lastName", lastName);
        
        // Update all fields simultaneously
        return usersRef.child(userId).updateChildren(updates);
    }

    /**
     * Change user password
     * @param currentPassword The user's current password
     * @param newPassword The new password to set
     * @return Task representing the result of the password change operation
     */
    public Task<Void> changePassword(String currentPassword, String newPassword) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
            taskCompletionSource.setException(new Exception("User not authenticated"));
            return taskCompletionSource.getTask();
        }

        // Re-authenticate user first (required by Firebase for security-sensitive operations)
        return firebaseAuth.signInWithEmailAndPassword(user.getEmail(), currentPassword)
            .continueWithTask(signInTask -> {
                if (signInTask.isSuccessful()) {
                    // User re-authenticated successfully, now update password
                    return user.updatePassword(newPassword);
                } else {
                    // Re-authentication failed
                    throw signInTask.getException();
                }
            });
    }
    
    /**
     * Send password reset email to the specified email address
     * @param email Email address to send reset link to
     * @return Task representing the result of the password reset email operation
     */
    public Task<Void> sendPasswordResetEmail(String email) {
        return firebaseAuth.sendPasswordResetEmail(email);
    }

    // Fetch and cache the default avatar URL
    private void fetchDefaultAvatarUrl() {
        DatabaseReference defaultAvatarRef = FirebaseDatabase.getInstance().getReference("general_information").child("default_avatar");
        defaultAvatarRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    defaultAvatarUrl = dataSnapshot.getValue(String.class);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.err.println("Error fetching default avatar URL: " + databaseError.getMessage());
            }
        });
    }

    // Get the default profile picture URL (as a Task to handle asynchronous nature)
    public Task<String> getDefaultProfilePicUrl() {
        TaskCompletionSource<String> taskCompletionSource = new TaskCompletionSource<>();
        
        // If we already have the default avatar URL cached, return it immediately
        if (!defaultAvatarUrl.isEmpty()) {
            taskCompletionSource.setResult(defaultAvatarUrl);
            return taskCompletionSource.getTask();
        }
        
        // Otherwise, fetch it from the database
        DatabaseReference defaultAvatarRef = FirebaseDatabase.getInstance().getReference("general_information").child("default_avatar");
        defaultAvatarRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String url = dataSnapshot.getValue(String.class);
                    defaultAvatarUrl = url; // Cache the URL
                    taskCompletionSource.setResult(url);
                } else {
                    taskCompletionSource.setResult(""); // Return empty string if not found
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                taskCompletionSource.setException(databaseError.toException());
            }
        });
        
        return taskCompletionSource.getTask();
    }

    /**
     * Update user's profile picture URL in Firebase Database
     * @param userId User ID whose profile picture needs to be updated
     * @param newProfilePicUrl URL of the new profile picture
     * @return Task representing the result of the update operation
     */
    public Task<Void> updateProfilePicture(String userId, String newProfilePicUrl) {
        // Create a map with just the field to update
        Map<String, Object> updates = new HashMap<>();
        updates.put("profilePicUrl", newProfilePicUrl);
        
        // Update the field
        return usersRef.child(userId).updateChildren(updates);
    }

    /**
     * Get the current user's profile picture URL
     * @param userId User ID to get profile picture for
     * @return Task that resolves with the profile picture URL
     */
    public Task<String> getCurrentProfilePicUrl(String userId) {
        TaskCompletionSource<String> taskCompletionSource = new TaskCompletionSource<>();
        
        usersRef.child(userId).child("profilePicUrl").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String url = task.getResult().getValue(String.class);
                taskCompletionSource.setResult(url != null ? url : "");
            } else {
                taskCompletionSource.setResult("");
            }
        });
        
        return taskCompletionSource.getTask();
    }

    public boolean isDefaultProfilePicture(String profilePicUrl) {
        return profilePicUrl != null && profilePicUrl.equals(defaultAvatarUrl);
    }

    public boolean containsDefaultProfileString(String url) {
        return url != null && url.contains("default_profile_d340av");
    }
}
