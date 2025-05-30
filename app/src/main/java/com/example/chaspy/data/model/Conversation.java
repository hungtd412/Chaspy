package com.example.chaspy.data.model;

public class Conversation {
    private String conversationId;
    private String lastMessage;
    private String lastMessageTime;
    private String friendId;
    private String friendUsername;
    private String profilePicUrl;
    private String themeColor;

    public Conversation() {
        // Empty constructor needed for Firebase
    }

    public Conversation(String conversationId, String lastMessage, String lastMessageTime, String friendId, String friendUsername, String profilePicUrl) {
        this.conversationId = conversationId;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.friendId = friendId;
        this.friendUsername = friendUsername;
        this.profilePicUrl = profilePicUrl;
    }

    public Conversation(String conversationId, String lastMessage, String lastMessageTime, String friendId, String friendUsername, String profilePicUrl, String themeColor) {
        this.conversationId = conversationId;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.friendId = friendId;
        this.friendUsername = friendUsername;
        this.profilePicUrl = profilePicUrl;
        this.themeColor = themeColor;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(String lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public String getFriendId() {
        return friendId;
    }

    public void setFriendId(String friendId) {
        this.friendId = friendId;
    }

    public String getFriendUsername() {
        return friendUsername;
    }

    public void setFriendUsername(String friendUsername) {
        this.friendUsername = friendUsername;
    }

    public String getProfilePicUrl() {
        return profilePicUrl;
    }

    public void setProfilePicUrl(String profilePicUrl) {
        this.profilePicUrl = profilePicUrl;
    }

    public String getThemeColor() {
        return themeColor;
    }

    public void setThemeColor(String themeColor) {
        this.themeColor = themeColor;
    }
}
