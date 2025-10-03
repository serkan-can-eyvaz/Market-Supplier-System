package com.example.marketsupplier.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class CreateOrderRequest {
    
    @NotEmpty(message = "Order items cannot be empty")
    @Valid
    private List<OrderItemRequest> orderItems;
    
    public CreateOrderRequest() {}
    
    public CreateOrderRequest(List<OrderItemRequest> orderItems) {
        this.orderItems = orderItems;
    }
    
    public List<OrderItemRequest> getOrderItems() {
        return orderItems;
    }
    
    public void setOrderItems(List<OrderItemRequest> orderItems) {
        this.orderItems = orderItems;
    }
}
