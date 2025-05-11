package com.example.chaspy.network;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthResult;
import com.google.android.gms.tasks.Task;
import com.example.chaspy.model.User;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class UserService {

    private static FirebaseAuth firebaseAuth;
    private static DatabaseReference usersRef;

    public UserService() {
        firebaseAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");
    }

    public Task<AuthResult> registerUser(String email, String password) {
        return firebaseAuth.createUserWithEmailAndPassword(email, password);
    }

    public Task<AuthResult> signInUser(String email, String password) {
        return firebaseAuth.signInWithEmailAndPassword(email, password);
    }

    // Save additional user data in Firestore
    public Task<Void> saveUserData(FirebaseUser firebaseUser, String firstName, String lastName) {
        User user = new User(firstName, lastName, firebaseUser.getEmail());
        return usersRef
                .child(firebaseUser.getUid())
                .setValue(user);
    }
}
