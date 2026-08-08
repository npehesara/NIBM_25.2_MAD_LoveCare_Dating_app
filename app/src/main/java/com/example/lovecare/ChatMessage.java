package com.example.lovecare;

public class ChatMessage {
    private String text;
    private String time;
    private boolean isSentByMe;

    // Required empty constructor for Firebase Realtime Database
    public ChatMessage() {
    }

    public ChatMessage(String text, String time, boolean isSentByMe) {
        this.text = text;
        this.time = time;
        this.isSentByMe = isSentByMe;
    }

    public String getText() { return text; }
    public String getTime() { return time; }
    public boolean isSentByMe() { return isSentByMe; }

    public void setText(String text) { this.text = text; }
    public void setTime(String time) { this.time = time; }
    public void setSentByMe(boolean sentByMe) { isSentByMe = sentByMe; }
}
