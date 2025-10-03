package com.example.marketsupplier.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "cart_items")
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit", nullable = false)
    private String unit;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    public CartItem() {}
    public CartItem(Cart cart, Long productId, String productName, Integer quantity, String unit, BigDecimal price) {
        this.cart = cart; this.productId = productId; this.productName = productName; this.quantity = quantity; this.unit = unit; this.price = price;
    }

    // Getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Cart getCart() { return cart; }
    public void setCart(Cart cart) { this.cart = cart; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}


