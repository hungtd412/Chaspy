package com.example.chaspy.data.model;

public class Conversation {
    private String conversationId;
    private String friendUsername;
    private String profilePicUrl;
    private String lastMessage;
    private String lastMessageTime;

    public Conversation(String conversationId, String friendUsername, String profilePicUrl, String lastMessage, String lastMessageTime) {
        this.conversationId = conversationId;
        this.friendUsername = friendUsername;
        this.profilePicUrl = profilePicUrl;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
    }

    // Getters for each field
    public String getConversationId() {
        return conversationId;
    }

    public String getFriendUsername() {
        return friendUsername;
    }

    public String getProfilePicUrl() {
        return profilePicUrl;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public String getLastMessageTime() {
        return lastMessageTime;
    }
}
