package com.example.chaspy.data.model;

import com.google.firebase.database.PropertyName;

public class FriendRequest {
    private String request_id;
    private String sender_id;
    private String receiver_id;
    private String status;
    private String sender_name;
    private String sender_profile_pic_url;

    public FriendRequest() {
        // Required empty constructor for Firebase
    }

    public FriendRequest(String sender_id, String receiver_id, String status) {
        this.sender_id = sender_id;
        this.receiver_id = receiver_id;
        this.status = status;
    }

    public FriendRequest(String request_id, String sender_id, String receiver_id, String status) {
        this.request_id = request_id;
        this.sender_id = sender_id;
        this.receiver_id = receiver_id;
        this.status = status;
    }

    // Getter and setters with @PropertyName annotations to map between Java and Firebase naming
    
    public String getRequestId() {
        return request_id;
    }

    public void setRequestId(String request_id) {
        this.request_id = request_id;
    }

    @PropertyName("sender_id")
    public String getSenderId() {
        return sender_id;
    }

    @PropertyName("sender_id")
    public void setSenderId(String sender_id) {
        this.sender_id = sender_id;
    }

    @PropertyName("receiver_id")
    public String getReceiverId() {
        return receiver_id;
    }

    @PropertyName("receiver_id")
    public void setReceiverId(String receiver_id) {
        this.receiver_id = receiver_id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSenderName() {
        return sender_name;
    }

    public void setSenderName(String sender_name) {
        this.sender_name = sender_name;
    }

    public String getSenderProfilePicUrl() {
        return sender_profile_pic_url;
    }

    public void setSenderProfilePicUrl(String sender_profile_pic_url) {
        this.sender_profile_pic_url = sender_profile_pic_url;
    }
}
