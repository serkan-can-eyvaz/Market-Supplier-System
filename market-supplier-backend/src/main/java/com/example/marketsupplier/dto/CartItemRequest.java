package com.example.marketsupplier.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CartItemRequest {
    
    @NotNull(message = "Ürün ID boş olamaz")
    private Long productId;
    
    @NotNull(message = "Miktar boş olamaz")
    @Min(value = 1, message = "Miktar 1 veya daha büyük olmalıdır")
    private Integer quantity;
    
    // Constructors
    public CartItemRequest() {}
    
    public CartItemRequest(Long productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }
    
    // Getters and Setters
    public Long getProductId() {
        return productId;
    }
    
    public void setProductId(Long productId) {
        this.productId = productId;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
