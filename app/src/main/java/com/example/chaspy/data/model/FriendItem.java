package com.example.chaspy.data.model;

public class FriendItem {
    private String uid;
    private String name;
    private String email;
    private String profileImageUrl;
    private boolean isActive;

    public FriendItem() {
        // Required empty constructor for Firebase
    }

    public FriendItem(String uid, String name, String email, String profileImageUrl, boolean isActive) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.isActive = isActive;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
