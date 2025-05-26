package com.example.chaspy.data.model;

public class Message {
    private String messageId;
    private String senderId;
    private String messageContent;
    private String messageType;
    private String timestamp;
    
    // Empty constructor needed for Firebase
    public Message() {
    }
    
    public Message(String messageId, String senderId, String messageContent, String messageType, String timestamp) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.messageContent = messageContent;
        this.messageType = messageType;
        this.timestamp = timestamp;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public String getSenderId() {
        return senderId;
    }
    
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
    
    public String getMessageContent() {
        return messageContent;
    }
    
    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }
    
    public String getMessageType() {
        return messageType;
    }
    
    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
