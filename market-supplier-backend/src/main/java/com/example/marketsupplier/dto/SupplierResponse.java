package com.example.marketsupplier.dto;

import java.time.LocalDateTime;

public class SupplierResponse {
    
    private Long id;
    private String companyName;
    private String phone;
    private String address;
    private String phoneNumberId;
    private Long userId;
    private String userName;
    private String userEmail;
    private LocalDateTime createdAt;
    private Long deliveryCount;
    
    // Constructors
    public SupplierResponse() {}
    
    public SupplierResponse(Long id, String companyName, String phone, String address, Long userId, 
                           String userName, String userEmail, LocalDateTime createdAt) {
        this.id = id;
        this.companyName = companyName;
        this.phone = phone;
        this.address = address;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.createdAt = createdAt;
    }
    
    public SupplierResponse(Long id, String companyName, String phone, String address, String phoneNumberId, 
                           Long userId, String userName, String userEmail, LocalDateTime createdAt) {
        this.id = id;
        this.companyName = companyName;
        this.phone = phone;
        this.address = address;
        this.phoneNumberId = phoneNumberId;
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
    
    public String getCompanyName() {
        return companyName;
    }
    
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getPhoneNumberId() { return phoneNumberId; }
    public void setPhoneNumberId(String phoneNumberId) { this.phoneNumberId = phoneNumberId; }
    
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
    
    public Long getDeliveryCount() {
        return deliveryCount;
    }
    
    public void setDeliveryCount(Long deliveryCount) {
        this.deliveryCount = deliveryCount;
    }
}
