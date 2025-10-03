package com.example.marketsupplier.dto;

import java.math.BigDecimal;

public class OrderItemResponse {
    
    private Long id;
    private String productName;
    private Integer quantity;
    private String unit;
    private BigDecimal price;
    private BigDecimal totalPrice;
    
    // Constructors
    public OrderItemResponse() {}
    
    public OrderItemResponse(Long id, String productName, Integer quantity, String unit, BigDecimal price) {
        this.id = id;
        this.productName = productName;
        this.quantity = quantity;
        this.unit = unit;
        this.price = price;
        this.totalPrice = price.multiply(BigDecimal.valueOf(quantity));
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
    
    public BigDecimal getTotalPrice() {
        return totalPrice;
    }
    
    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
}
