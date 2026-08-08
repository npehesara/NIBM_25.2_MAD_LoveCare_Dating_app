package com.example.lovecare;

public class ChatItem {
    private String userId;
    private String username;
    private String lastMessage;
    private String time;
    private String photoUrl;
    private int avatarResId;
    private boolean isOnline;

    public ChatItem(String userId, String username, String lastMessage, String time, String photoUrl, int avatarResId, boolean isOnline) {
        this.userId = userId;
        this.username = username;
        this.lastMessage = lastMessage;
        this.time = time;
        this.photoUrl = photoUrl;
        this.avatarResId = avatarResId;
        this.isOnline = isOnline;
    }

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getLastMessage() { return lastMessage; }
    public String getTime() { return time; }
    public String getPhotoUrl() { return photoUrl; }
    public int getAvatarResId() { return avatarResId; }
    public boolean isOnline() { return isOnline; }
}
