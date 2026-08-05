package com.example.lovecare;

public class NotificationItem {
    private String title;
    private String message;
    private String timeAgo;
    private int iconResId;

    public NotificationItem(String title, String message, String timeAgo, int iconResId) {
        this.title = title;
        this.message = message;
        this.timeAgo = timeAgo;
        this.iconResId = iconResId;
    }

    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getTimeAgo() { return timeAgo; }
    public int getIconResId() { return iconResId; }
}
