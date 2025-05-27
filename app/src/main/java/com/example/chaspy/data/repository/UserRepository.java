package com.example.chaspy.data.repository;

import com.example.chaspy.data.service.UserFirebaseService;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.tasks.Task;

public class UserRepository {

    private UserFirebaseService userFirebaseService;

    public UserRepository() {
        userFirebaseService = new UserFirebaseService();
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
    
    // Resend email verification
    public Task<Void> resendVerificationEmail() {
        return userFirebaseService.resendVerificationEmail();
    }

    // Save user data
    public Task<Void> saveUserData(Task<AuthResult> task, String firstName, String lastName) {
        FirebaseUser firebaseUser = task.getResult().getUser();
        return userFirebaseService.saveUserData(firebaseUser, firstName, lastName);
    }
}
