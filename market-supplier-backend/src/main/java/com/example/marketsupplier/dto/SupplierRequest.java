package com.example.marketsupplier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SupplierRequest {
    
    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters")
    private String companyName;
    
    @NotBlank(message = "Phone is required")
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;
    
    private String address;
    
    @Size(max = 50, message = "Phone number ID must not exceed 50 characters")
    private String phoneNumberId;
    
    // Constructors
    public SupplierRequest() {}
    
    public SupplierRequest(String companyName, String phone) {
        this.companyName = companyName;
        this.phone = phone;
    }
    
    public SupplierRequest(String companyName, String phone, String address, String phoneNumberId) {
        this.companyName = companyName;
        this.phone = phone;
        this.address = address;
        this.phoneNumberId = phoneNumberId;
    }
    
    // Getters and Setters
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
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getPhoneNumberId() {
        return phoneNumberId;
    }
    
    public void setPhoneNumberId(String phoneNumberId) {
        this.phoneNumberId = phoneNumberId;
    }
}
