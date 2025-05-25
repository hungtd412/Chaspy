package com.example.chaspy.data.model;

public class FriendRequest {
    private String requestId;
    private String senderId;
    private String receiverId;
    private String status;
    private String senderName;
    private String senderProfilePicUrl;

    public FriendRequest() {
        // Required empty constructor for Firebase
    }

    public FriendRequest(String requestId, String senderId, String receiverId, String status) {
        this.requestId = requestId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.status = status;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderProfilePicUrl() {
        return senderProfilePicUrl;
    }

    public void setSenderProfilePicUrl(String senderProfilePicUrl) {
        this.senderProfilePicUrl = senderProfilePicUrl;
    }
}
