package com.example.chaspy.data.model;

import com.google.firebase.database.Exclude;

public class ScheduleMessage {
    private String id;
    private String sender_id;
    private String receiver_id;
    private String message_content;
    private long sending_time; // We'll keep this as long in Java for easier date handling
    
    // Empty constructor needed for Firebase
    public ScheduleMessage() {
    }
    
    public ScheduleMessage(String id, String sender_id, String receiver_id, String message_content, long sending_time) {
        this.id = id;
        this.sender_id = sender_id;
        this.receiver_id = receiver_id;
        this.message_content = message_content;
        this.sending_time = sending_time;
    }
    
    // Constructor that takes sending_time as String
    public ScheduleMessage(String id, String sender_id, String receiver_id, String message_content, String sending_time) {
        this.id = id;
        this.sender_id = sender_id;
        this.receiver_id = receiver_id;
        this.message_content = message_content;
        setSendingTimeFromString(sending_time);
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getSenderId() {
        return sender_id;
    }
    
    public void setSenderId(String sender_id) {
        this.sender_id = sender_id;
    }
    
    public String getReceiverId() {
        return receiver_id;
    }
    
    public void setReceiverId(String receiver_id) {
        this.receiver_id = receiver_id;
    }
    
    public String getMessageContent() {
        return message_content;
    }
    
    public void setMessageContent(String message_content) {
        this.message_content = message_content;
    }
    
    @Exclude // This annotation tells Firebase not to map this directly
    public long getSendingTime() {
        return sending_time;
    }
    
    @Exclude // This annotation tells Firebase not to map this directly
    public void setSendingTime(long sending_time) {
        this.sending_time = sending_time;
    }
    
    // Method to get sending time as string for Firebase
    public String getSending_time() {
        return String.valueOf(sending_time);
    }
    
    // Method to set sending time from string from Firebase
    public void setSending_time(String timeStr) {
        setSendingTimeFromString(timeStr);
    }
    
    private void setSendingTimeFromString(String timeStr) {
        try {
            this.sending_time = Long.parseLong(timeStr);
        } catch (NumberFormatException e) {
            // Default to current time if parsing fails
            this.sending_time = System.currentTimeMillis();
        }
    }
}
