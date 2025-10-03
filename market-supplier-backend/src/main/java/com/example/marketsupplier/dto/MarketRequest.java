package com.example.marketsupplier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MarketRequest {
    
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;
    
    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;
    
    @NotBlank(message = "Phone is required")
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;
    
    // Constructors
    public MarketRequest() {}
    
    public MarketRequest(String name, String address, String phone) {
        this.name = name;
        this.address = address;
        this.phone = phone;
    }
    
    // Getters and Setters
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
}
