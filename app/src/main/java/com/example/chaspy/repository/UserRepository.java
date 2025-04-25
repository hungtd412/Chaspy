package com.example.chaspy.repository;

import com.example.chaspy.network.UserService;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.tasks.Task;

public class UserRepository {

    private UserService userService;

    public UserRepository() {
        userService = new UserService();
    }

    // Register a new user
    public Task<AuthResult> registerUser(String email, String password) {
        return userService.registerUser(email, password);
    }

    // Sign in the user
    public Task<AuthResult> signInUser(String email, String password) {
        return userService.signInUser(email, password);
    }

    // Save user data
    public Task<Void> saveUserData(FirebaseUser firebaseUser, String firstName, String lastName) {
        return userService.saveUserData(firebaseUser, firstName, lastName);
    }
}
