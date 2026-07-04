package com.firstapp.da_messaging_module.modules;

public class User {
    private String id;
    private String username;
    // You can add profile image URLs later

    public User(String id, String username) {
        this.id = id;
        this.username = username;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }

}
