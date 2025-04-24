package com.example.chaspy.network;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseService {

    // Get a specific reference (e.g., users, posts)
    public static DatabaseReference getReference(String node) {
        return FirebaseDatabase.getInstance().getReference(node);
    }
}
