package com.example.lovecare;

public class ChatMessage {
    private String text;
    private String time;
    private boolean isSentByMe;

    public ChatMessage(String text, String time, boolean isSentByMe) {
        this.text = text;
        this.time = time;
        this.isSentByMe = isSentByMe;
    }

    public String getText() { return text; }
    public String getTime() { return time; }
    public boolean isSentByMe() { return isSentByMe; }
}
