package com.example.admindashboardactivity;

/**
 * Model class representing an announcement created by an admin.
 */
public class AdminAnnouncement {

    private String id;
    private String title;
    private String message;
    private long createdAt;

    public AdminAnnouncement() {
        // Required empty constructor for Firestore
    }

    public AdminAnnouncement(String id, String title, String message, long createdAt) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public long getCreatedAt() { return createdAt; }

    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setMessage(String message) { this.message = message; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
