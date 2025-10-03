package com.example.marketsupplier.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;
    
    @NotBlank
    @Size(min = 1, max = 100)
    @Column(name = "product_name", nullable = false)
    private String productName;
    
    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(nullable = false)
    private Integer quantity;
    
    @NotBlank
    @Size(max = 20)
    @Column(nullable = false)
    private String unit;
    
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be greater than or equal to 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    // Constructors
    public OrderItem() {
    }
    
    public OrderItem(Order order, String productName, Integer quantity, String unit, BigDecimal price) {
        this.order = order;
        this.productName = productName;
        this.quantity = quantity;
        this.unit = unit;
        this.price = price;
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
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public String getUnit() {
        return unit;
    }
    
    public void setUnit(String unit) {
        this.unit = unit;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    // Helper method to calculate total price for this item
    public BigDecimal getTotalPrice() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
