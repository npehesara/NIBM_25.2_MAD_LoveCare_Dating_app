package com.example.lovecare;

public class ChatMessage {
    private String text;
    private String time;
    private boolean isSentByMe;
    private String imageUrl;   // Cloudinary URL for image messages

    // Required empty constructor for Firebase Realtime Database
    public ChatMessage() {
    }

    public ChatMessage(String text, String time, boolean isSentByMe) {
        this.text = text;
        this.time = time;
        this.isSentByMe = isSentByMe;
        this.imageUrl = null;
    }

    public ChatMessage(String text, String time, boolean isSentByMe, String imageUrl) {
        this.text = text;
        this.time = time;
        this.isSentByMe = isSentByMe;
        this.imageUrl = imageUrl;
    }

    public String getText() { return text; }
    public String getTime() { return time; }
    public boolean isSentByMe() { return isSentByMe; }
    public String getImageUrl() { return imageUrl; }

    public void setText(String text) { this.text = text; }
    public void setTime(String time) { this.time = time; }
    public void setSentByMe(boolean sentByMe) { isSentByMe = sentByMe; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isImageMessage() {
        return imageUrl != null && !imageUrl.isEmpty();
    }
}
