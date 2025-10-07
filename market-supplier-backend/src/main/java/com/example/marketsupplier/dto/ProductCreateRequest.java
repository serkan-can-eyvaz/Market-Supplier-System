package com.example.marketsupplier.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class ProductCreateRequest {
    
    @NotBlank(message = "Ürün adı boş olamaz")
    @Size(min = 1, max = 100, message = "Ürün adı 1-100 karakter arasında olmalıdır")
    private String name;
    
    @Size(max = 500, message = "Açıklama en fazla 500 karakter olabilir")
    private String description;
    
    @NotBlank(message = "Birim boş olamaz")
    @Size(max = 20, message = "Birim en fazla 20 karakter olabilir")
    private String unit;
    
    @NotNull(message = "Fiyat boş olamaz")
    @DecimalMin(value = "0.0", inclusive = true, message = "Fiyat 0 veya daha büyük olmalıdır")
    private BigDecimal price;
    
    @NotNull(message = "Stok miktarı boş olamaz")
    @Min(value = 0, message = "Stok miktarı 0 veya daha büyük olmalıdır")
    private Integer stockQuantity;
    
    // Constructors
    public ProductCreateRequest() {}
    
    public ProductCreateRequest(String name, String description, String unit, BigDecimal price, Integer stockQuantity) {
        this.name = name;
        this.description = description;
        this.unit = unit;
        this.price = price;
        this.stockQuantity = stockQuantity;
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
}
