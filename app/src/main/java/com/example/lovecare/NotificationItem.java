package com.example.lovecare;

public class NotificationItem {
    private String title;
    private String message;
    private String timeAgo;
    private int iconResId;
    private String type;
    private String fromUid;

    public NotificationItem(String title, String message, String timeAgo, int iconResId) {
        this.title = title;
        this.message = message;
        this.timeAgo = timeAgo;
        this.iconResId = iconResId;
    }

    public NotificationItem(String title, String message, String timeAgo, int iconResId, String type, String fromUid) {
        this.title = title;
        this.message = message;
        this.timeAgo = timeAgo;
        this.iconResId = iconResId;
        this.type = type;
        this.fromUid = fromUid;
    }

    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getTimeAgo() { return timeAgo; }
    public int getIconResId() { return iconResId; }
    public String getType() { return type; }
    public String getFromUid() { return fromUid; }
}
