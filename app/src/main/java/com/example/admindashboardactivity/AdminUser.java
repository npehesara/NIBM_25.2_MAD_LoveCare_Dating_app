package com.example.admindashboardactivity;

/**
 * Model class representing a user in the admin panel.
 * Maps directly to documents in the Firestore "users" collection.
 */
public class AdminUser {

    private String uid;
    private String name;
    private String email;
    private String role;
    private String gender;
    private String age;
    private String photoUrl;
    private long createdAt;

    public AdminUser() {
        // Required empty constructor for Firestore deserialization
    }

    public AdminUser(String uid, String name, String email, String role) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getGender() {
        return gender;
    }

    public String getAge() {
        return age;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
