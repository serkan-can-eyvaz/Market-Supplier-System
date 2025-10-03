package com.example.marketsupplier.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "markets")
public class Market {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;
    
    @NotBlank
    @Size(min = 2, max = 100)
    @Column(nullable = false)
    private String name;
    
    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String address;
    
    @NotBlank
    @Size(max = 20)
    @Column(nullable = false)
    private String phone;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "latitude")
    private Double latitude;
    
    @Column(name = "longitude") 
    private Double longitude;
    
    @OneToMany(mappedBy = "market", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Order> orders;
    
    // Constructors
    public Market() {
        this.createdAt = LocalDateTime.now();
    }
    
    public Market(User user, String name, String address, String phone) {
        this();
        this.user = user;
        this.name = name;
        this.address = address;
        this.phone = phone;
    }
    
    public Market(User user, String name, String address, String phone, Double latitude, Double longitude) {
        this();
        this.user = user;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
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
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public List<Order> getOrders() {
        return orders;
    }
    
    public void setOrders(List<Order> orders) {
        this.orders = orders;
    }
    
    public Double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }
    
    public Double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
