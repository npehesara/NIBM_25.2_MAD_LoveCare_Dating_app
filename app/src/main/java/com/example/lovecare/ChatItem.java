package com.example.lovecare;

public class ChatItem {
    private String username;
    private String lastMessage;
    private String time;
    private int avatarResId;
    private boolean isOnline;

    public ChatItem(String username, String lastMessage, String time, int avatarResId, boolean isOnline) {
        this.username = username;
        this.lastMessage = lastMessage;
        this.time = time;
        this.avatarResId = avatarResId;
        this.isOnline = isOnline;
    }

    public String getUsername() { return username; }
    public String getLastMessage() { return lastMessage; }
    public String getTime() { return time; }
    public int getAvatarResId() { return avatarResId; }
    public boolean isOnline() { return isOnline; }
}
