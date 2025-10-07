package com.example.marketsupplier.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class ProductUpdateRequest {
    
    @Size(min = 1, max = 100, message = "Ürün adı 1-100 karakter arasında olmalıdır")
    private String name;
    
    @Size(max = 500, message = "Açıklama en fazla 500 karakter olabilir")
    private String description;
    
    @Size(max = 20, message = "Birim en fazla 20 karakter olabilir")
    private String unit;
    
    @DecimalMin(value = "0.0", inclusive = true, message = "Fiyat 0 veya daha büyük olmalıdır")
    private BigDecimal price;
    
    @Min(value = 0, message = "Stok miktarı 0 veya daha büyük olmalıdır")
    private Integer stockQuantity;
    
    private Boolean isActive;
    
    // Constructors
    public ProductUpdateRequest() {}
    
    public ProductUpdateRequest(String name, String description, String unit, BigDecimal price, Integer stockQuantity, Boolean isActive) {
        this.name = name;
        this.description = description;
        this.unit = unit;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.isActive = isActive;
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
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
    
    public Integer getStockQuantity() {
        return stockQuantity;
    }
    
    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
