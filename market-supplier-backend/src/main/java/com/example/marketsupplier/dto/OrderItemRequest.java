package com.example.marketsupplier.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class OrderItemRequest {
    private Long productId; // optional, varsa ID öncelikli
    
    @NotBlank(message = "Product name is required")
    @Size(min = 1, max = 100, message = "Product name must be between 1 and 100 characters")
    private String productName;
    
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
    
    @NotBlank(message = "Unit is required")
    @Size(max = 20, message = "Unit must not exceed 20 characters")
    private String unit;
    
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;
    
    // Constructors
    public OrderItemRequest() {}
    
    public OrderItemRequest(String productName, Integer quantity, String unit, BigDecimal price) {
        this.productName = productName;
        this.quantity = quantity;
        this.unit = unit;
        this.price = price;
    }

    public OrderItemRequest(Long productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }
    
    // Getters and Setters
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

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
}
