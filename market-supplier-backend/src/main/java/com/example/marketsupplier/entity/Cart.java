package com.example.marketsupplier.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "market_id", nullable = false)
    private Market market;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Cart() {}

    public Cart(Market market) {
        this.market = market;
    }

    @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    // Getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Market getMarket() { return market; }
    public void setMarket(Market market) { this.market = market; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}


