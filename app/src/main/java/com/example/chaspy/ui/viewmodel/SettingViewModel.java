package com.example.chaspy.ui.viewmodel;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.chaspy.data.model.User;
import com.example.chaspy.data.repository.UserRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SettingViewModel extends ViewModel {
    private static final String TAG = "SettingViewModel";


    private final UserRepository userRepository;
    private final FirebaseAuth firebaseAuth;
    private final DatabaseReference usersRef;
    private final DatabaseReference generalInfoRef;
    
    private final MutableLiveData<User> userData = new MutableLiveData<>();
    private final MutableLiveData<String> defaultAvatarUrl = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    
    public SettingViewModel() {
        this.userRepository = new UserRepository();
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.usersRef = FirebaseDatabase.getInstance().getReference("users");
        this.generalInfoRef = FirebaseDatabase.getInstance().getReference("general_information");
        loadDefaultAvatarUrl();
    }
    
    public LiveData<User> getUserData() {
        return userData;
    }
    
    public LiveData<String> getDefaultAvatarUrl() {
        return defaultAvatarUrl;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public String getCurrentUserId() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            return currentUser.getUid();
        }
        return null;
    }
    
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }
    
    public boolean isUserLoggedIn() {
        return getCurrentUser() != null;
    }
    
    public void loadUserData() {
        String userId = getCurrentUserId();
        if (userId == null) {
            errorMessage.setValue("User not logged in");
            return;
        }
        
        usersRef.child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    try {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            user.setUid(userId);
                            userData.setValue(user);
                        } else {
                            errorMessage.setValue("Failed to parse user data");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing user data", e);
                        errorMessage.setValue("Error parsing user data: " + e.getMessage());
                    }
                } else {
                    errorMessage.setValue("User data not found in the database");
                }
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                errorMessage.setValue("Database error: " + error.getMessage());
            }
        });
    }
    
    private void loadDefaultAvatarUrl() {
        generalInfoRef.child("default_avatar").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String url = snapshot.getValue(String.class);
                if (url != null && !url.isEmpty()) {
                    defaultAvatarUrl.setValue(url);
                } else {
                    Log.w(TAG, "Default avatar URL not found in database");
                }
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Database error when loading default avatar: " + error.getMessage());
            }
        });
    }

    public Task<Void> updateUserName(String firstName, String lastName) {
        String userId = getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("User not logged in");
        }
        return userRepository.updateUserProfile(userId, firstName, lastName);
    }
    
    public Task<Void> changePassword(String currentPassword, String newPassword) {
        return userRepository.changePassword(currentPassword, newPassword);
    }
    
    public Task<Void> sendPasswordResetEmail(String email) {
        System.out.println("Sending password reset email to: " + email);
        return userRepository.sendPasswordResetEmail(email);
    }
    
    public void logout() {
        userRepository.signOut();
    }
    
    public Task<String> uploadProfileImage(Context context, Uri imageUri) {
        isLoading.setValue(true);
        String userId = getCurrentUserId();
        if (userId == null) {
            isLoading.setValue(false);
            throw new IllegalStateException("User not logged in");
        }
        
        // Get current user data to access the current profile picture URL
        User currentUser = userData.getValue();
        String oldProfilePicUrl = currentUser != null ? currentUser.getProfilePicUrl() : null;
        
        return userRepository.uploadImageToCloudinary(context, imageUri)
            .continueWithTask(uploadTask -> {
                if (!uploadTask.isSuccessful()) {
                    throw uploadTask.getException();
                }
                
                String newImageUrl = uploadTask.getResult();
                // After successful upload to Cloudinary, update the user's profile in Firebase
                return userRepository.updateProfilePicture(userId, newImageUrl)
                    .continueWithTask(updateTask -> {
                        if (!updateTask.isSuccessful()) {
                            throw updateTask.getException();
                        }
                        
                        // Only delete the old image if it exists and is not the default image
                        if (oldProfilePicUrl != null && !oldProfilePicUrl.isEmpty() && 
                            !userRepository.containsDefaultProfileString(oldProfilePicUrl)) {
                            
                            Log.d(TAG, "Deleting old profile image: " + oldProfilePicUrl);
                            return userRepository.deleteImageFromCloudinary(context, oldProfilePicUrl)
                                .continueWith(deleteTask -> {
                                    isLoading.setValue(false);
                                    if (!deleteTask.isSuccessful()) {
                                        Log.e(TAG, "Failed to delete old profile image", deleteTask.getException());
                                        // Still return the new URL even if deletion fails
                                    }
                                    return newImageUrl;
                                });
                        } else {
                            isLoading.setValue(false);
                            return Tasks.forResult(newImageUrl);
                        }
                    });
            });
    }
    
    public boolean isDefaultProfilePicture(String profilePicUrl) {
        return userRepository.isDefaultProfilePicture(profilePicUrl);
    }
    
    public boolean containsDefaultProfileString(String url) {
        return userRepository.containsDefaultProfileString(url);
    }
}
