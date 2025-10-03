package com.example.marketsupplier.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class UpdateOrderRequest {
    
    @NotEmpty(message = "Order items cannot be empty")
    @Valid
    private List<OrderItemRequest> orderItems;
    
    public UpdateOrderRequest() {}
    
    public UpdateOrderRequest(List<OrderItemRequest> orderItems) {
        this.orderItems = orderItems;
    }
    
    public List<OrderItemRequest> getOrderItems() {
        return orderItems;
    }
    
    public void setOrderItems(List<OrderItemRequest> orderItems) {
        this.orderItems = orderItems;
    }
}
