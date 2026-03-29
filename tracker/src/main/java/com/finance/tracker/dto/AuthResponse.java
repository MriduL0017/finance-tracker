package com.finance.tracker.dto;

public class AuthResponse {
    private String token;
    private Integer userId; // NEW
    private String name;    // NEW

    public AuthResponse(String token, Integer userId, String name) {
        this.token = token;
        this.userId = userId;
        this.name = name;
    }

    // Getters and Setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}