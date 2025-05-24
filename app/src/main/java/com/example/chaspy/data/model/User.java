package com.example.chaspy.data.model;

import java.util.HashMap;
import java.util.Map;

public class User {
    private String uid;
    private String email;
    private String firstName; // Changed from first_name
    private String lastName;  // Changed from last_name
    private boolean isActive; // Unchanged
    private String profilePicUrl;
    private Map<String, Boolean> conversations = new HashMap<>();
    private Map<String, Boolean> friends = new HashMap<>();
    private Map<String, Boolean> block_list = new HashMap<>(); // Unchanged

    public User() {
        // Required empty constructor for Firebase
    }

    public User(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isisActive() {
        return isActive;
    }

    public void setisActive(boolean isActive) {
        this.isActive = isActive;
    }

    public String getProfilePicUrl() {
        return profilePicUrl;
    }

    public void setProfilePicUrl(String profilePicUrl) {
        this.profilePicUrl = profilePicUrl;
    }

    public Map<String, Boolean> getConversations() {
        return conversations;
    }

    public void setConversations(Map<String, Boolean> conversations) {
        this.conversations = conversations;
    }

    public Map<String, Boolean> getFriends() {
        return friends;
    }

    public void setFriends(Map<String, Boolean> friends) {
        this.friends = friends;
    }
    
    public Map<String, Boolean> getBlock_list() {
        return block_list;
    }

    public void setBlock_list(Map<String, Boolean> block_list) {
        this.block_list = block_list;
    }

    public String getUserName() {
        return firstName + " " + lastName;
    }
}
