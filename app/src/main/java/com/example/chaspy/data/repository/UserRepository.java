package com.example.chaspy.data.repository;

import android.content.Context;
import android.net.Uri;

import com.example.chaspy.data.service.CloudinaryService;
import com.example.chaspy.data.service.UserFirebaseService;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.tasks.Task;

public class UserRepository {

    private UserFirebaseService userFirebaseService;
    private CloudinaryService cloudinaryService;

    public UserRepository() {
        userFirebaseService = new UserFirebaseService();
        cloudinaryService = new CloudinaryService();
    }

    // Register a new user
    public Task<AuthResult> registerUser(String email, String password) {
        return userFirebaseService.registerUser(email, password);
    }

    // Send email verification
    public Task<Void> sendEmailVerification(FirebaseUser user) {
        return userFirebaseService.sendEmailVerification(user);
    }
    
    // Check if email is verified
    public boolean isEmailVerified(FirebaseUser user) {
        return userFirebaseService.isEmailVerified(user);
    }

    // Sign in the user
    public Task<AuthResult> signInUser(String email, String password) {
        return userFirebaseService.signInUser(email, password);
    }
    
    // Sign out the current user
    public void signOut() {
        userFirebaseService.signOut();
    }
    
    // Resend verification email
    public Task<Void> resendVerificationEmail(String email, String password) {
        return userFirebaseService.resendVerificationEmail(email, password);
    }

    // Save user data
    public Task<Void> saveUserData(Task<AuthResult> task, String firstName, String lastName) {
        FirebaseUser firebaseUser = task.getResult().getUser();
        return userFirebaseService.saveUserData(firebaseUser, firstName, lastName);
    }

    public Task<Void> updateUserProfile(String userId, String firstName, String lastName) {
        return userFirebaseService.updateUserProfile(userId, firstName, lastName);
    }

    public Task<Void> changePassword(String currentPassword, String newPassword) {
        return userFirebaseService.changePassword(currentPassword, newPassword);
    }
    
    public Task<Void> sendPasswordResetEmail(String email) {
        return userFirebaseService.sendPasswordResetEmail(email);
    }

    public Task<Void> updateProfilePicture(String userId, String newProfilePicUrl) {
        return userFirebaseService.updateProfilePicture(userId, newProfilePicUrl);
    }

    public Task<String> getCurrentProfilePicUrl(String userId) {
        return userFirebaseService.getCurrentProfilePicUrl(userId);
    }
    
    public boolean isDefaultProfilePicture(String profilePicUrl) {
        return userFirebaseService.isDefaultProfilePicture(profilePicUrl);
    }
    
    public boolean containsDefaultProfileString(String url) {
        return userFirebaseService.containsDefaultProfileString(url);
    }
    
    // Upload image to Cloudinary
    public Task<String> uploadImageToCloudinary(Context context, Uri imageUri) {
        return cloudinaryService.uploadImage(context, imageUri);
    }
}
