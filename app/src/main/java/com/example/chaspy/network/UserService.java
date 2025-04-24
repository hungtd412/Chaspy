package com.example.chaspy.network;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthResult;
import com.google.android.gms.tasks.Task;
import com.example.chaspy.model.User;
import com.google.android.gms.tasks.OnCompleteListener;

public class UserService {

    private FirebaseAuth firebaseAuth;

    public UserService() {
        firebaseAuth = FirebaseAuth.getInstance();
    }

    // Register a new user with email and password
    public Task<AuthResult> registerUser(String email, String password) {
        return firebaseAuth.createUserWithEmailAndPassword(email, password);
    }

    // Save additional user data in Firestore
    public Task<Void> saveUserData(FirebaseUser firebaseUser, String firstName, String lastName) {
        User user = new User(firstName, lastName, firebaseUser.getEmail());
        return FirebaseService.getReference("users")
                .child(firebaseUser.getUid())
                .setValue(user);
    }
}
