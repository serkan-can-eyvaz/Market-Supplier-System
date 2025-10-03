package com.example.marketsupplier.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "market_id", nullable = false)
    @JsonIgnore
    private Market market;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "total_price")
    private java.math.BigDecimal totalPrice;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<OrderItem> items = new java.util.ArrayList<>();
    
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Delivery delivery;
    
    // Constructors
    public Order() {
        this.createdAt = LocalDateTime.now();
        this.status = OrderStatus.PENDING;
    }
    
    public Order(Market market) {
        this();
        this.market = market;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Market getMarket() {
        return market;
    }
    
    public void setMarket(Market market) {
        this.market = market;
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public void setStatus(OrderStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public List<OrderItem> getItems() {
        return items;
    }
    
    public void setItems(List<OrderItem> items) {
        this.items = items;
    }
    
    public java.math.BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(java.math.BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
    
    public Delivery getDelivery() {
        return delivery;
    }
    
    public void setDelivery(Delivery delivery) {
        this.delivery = delivery;
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = OrderStatus.PENDING;
        }
    }
}
