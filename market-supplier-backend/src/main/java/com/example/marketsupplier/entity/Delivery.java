package com.example.marketsupplier.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "deliveries")
public class Delivery {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false)
    private DeliveryStatus deliveryStatus;
    
    @Column(name = "delivery_time")
    private LocalDateTime deliveryTime;
    
    @Column(name = "estimated_delivery_time")
    private LocalDateTime estimatedDeliveryTime;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "route_info", columnDefinition = "TEXT")
    private String routeInfo; // JSON string containing route information
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public Delivery() {
        this.createdAt = LocalDateTime.now();
        this.deliveryStatus = DeliveryStatus.IN_PROGRESS;
    }
    
    public Delivery(Order order, Supplier supplier) {
        this();
        this.order = order;
        this.supplier = supplier;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Order getOrder() {
        return order;
    }
    
    public void setOrder(Order order) {
        this.order = order;
    }
    
    public Supplier getSupplier() {
        return supplier;
    }
    
    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }
    
    public DeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
    }
    
    public void setDeliveryStatus(DeliveryStatus deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }
    
    public LocalDateTime getDeliveryTime() {
        return deliveryTime;
    }
    
    public void setDeliveryTime(LocalDateTime deliveryTime) {
        this.deliveryTime = deliveryTime;
    }
    
    public LocalDateTime getEstimatedDeliveryTime() {
        return estimatedDeliveryTime;
    }

    public void setEstimatedDeliveryTime(LocalDateTime estimatedDeliveryTime) {
        this.estimatedDeliveryTime = estimatedDeliveryTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getRouteInfo() {
        return routeInfo;
    }
    
    public void setRouteInfo(String routeInfo) {
        this.routeInfo = routeInfo;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (deliveryStatus == null) {
            deliveryStatus = DeliveryStatus.IN_PROGRESS;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Helper method to mark delivery as completed
    public void markAsDelivered() {
        this.deliveryStatus = DeliveryStatus.DELIVERED;
        this.deliveryTime = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
