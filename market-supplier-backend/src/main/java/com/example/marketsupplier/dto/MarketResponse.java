package com.example.marketsupplier.dto;

import java.time.LocalDateTime;

public class MarketResponse {
    
    private Long id;
    private String name;
    private String address;
    private String phone;
    private Long userId;
    private String userName;
    private String userEmail;
    private LocalDateTime createdAt;
    private Long orderCount;
    
    // Constructors
    public MarketResponse() {}
    
    public MarketResponse(Long id, String name, String address, String phone, Long userId, 
                         String userName, String userEmail, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getUserEmail() {
        return userEmail;
    }
    
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public Long getOrderCount() {
        return orderCount;
    }
    
    public void setOrderCount(Long orderCount) {
        this.orderCount = orderCount;
    }
}
