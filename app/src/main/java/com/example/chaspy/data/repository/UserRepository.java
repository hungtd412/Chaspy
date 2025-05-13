package com.example.chaspy.repository;

import com.example.chaspy.network.UserFirebaseService;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.tasks.Task;

public class UserRepository {

    private UserFirebaseService UserFirebaseService;

    public UserRepository() {
        UserFirebaseService = new UserFirebaseService();
    }

    // Register a new user
    public Task<AuthResult> registerUser(String email, String password) {
        return UserFirebaseService.registerUser(email, password);
    }

    // Sign in the user
    public Task<AuthResult> signInUser(String email, String password) {
        return UserFirebaseService.signInUser(email, password);
    }

    // Save user data
    public Task<Void> saveUserData(Task<AuthResult> task, String firstName, String lastName) {
        FirebaseUser firebaseUser = task.getResult().getUser();

        return UserFirebaseService.saveUserData(firebaseUser, firstName, lastName);
    }
}
