package com.example.marketsupplier.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "suppliers")
public class Supplier {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore
    private User user;
    
    @NotBlank
    @Size(min = 2, max = 100)
    @Column(name = "company_name", nullable = false)
    private String companyName;
    
    @NotBlank
    @Size(max = 20)
    @Column(nullable = false)
    private String phone;
    
    @Column(name = "address")
    private String address;
    
    @Size(max = 50)
    @Column(name = "phone_number_id")
    private String phoneNumberId;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Delivery> deliveries;
    
    // Constructors
    public Supplier() {
        this.createdAt = LocalDateTime.now();
    }
    
    public Supplier(User user, String companyName, String phone) {
        this();
        this.user = user;
        this.companyName = companyName;
        this.phone = phone;
    }
    
    public Supplier(User user, String companyName, String phone, String phoneNumberId) {
        this();
        this.user = user;
        this.companyName = companyName;
        this.phone = phone;
        this.phoneNumberId = phoneNumberId;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
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
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public List<Delivery> getDeliveries() {
        return deliveries;
    }
    
    public void setDeliveries(List<Delivery> deliveries) {
        this.deliveries = deliveries;
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
