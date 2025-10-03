package com.example.marketsupplier.dto;

import jakarta.validation.constraints.NotNull;

public class DeliveryRequest {
    
    @NotNull(message = "Order ID is required")
    private Long orderId;
    
    @NotNull(message = "Supplier ID is required")
    private Long supplierId;
    
    // Constructors
    public DeliveryRequest() {}
    
    public DeliveryRequest(Long orderId, Long supplierId) {
        this.orderId = orderId;
        this.supplierId = supplierId;
    }
    
    // Getters and Setters
    public Long getOrderId() {
        return orderId;
    }
    
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    
    public Long getSupplierId() {
        return supplierId;
    }
    
    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }
}
