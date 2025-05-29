package com.example.chaspy.data.model;

public class ScheduleMessage {
    private String id;
    private String senderId;
    private String receiverId;
    private String messageContent;
    private long sendingTime;
    private String conversationId; // New field

    // Default constructor required for Firebase
    public ScheduleMessage() {
    }

    // Constructor without conversationId (for backward compatibility)
    public ScheduleMessage(String id, String senderId, String receiverId, String messageContent, long sendingTime) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.messageContent = messageContent;
        this.sendingTime = sendingTime;
        this.conversationId = null;
    }

    // Constructor with conversationId
    public ScheduleMessage(String id, String senderId, String receiverId, String messageContent, long sendingTime, String conversationId) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.messageContent = messageContent;
        this.sendingTime = sendingTime;
        this.conversationId = conversationId;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public long getSendingTime() {
        return sendingTime;
    }

    public void setSendingTime(long sendingTime) {
        this.sendingTime = sendingTime;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
}
