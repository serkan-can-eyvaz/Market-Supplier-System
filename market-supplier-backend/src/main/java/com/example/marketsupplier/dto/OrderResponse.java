package com.example.marketsupplier.dto;

import com.example.marketsupplier.entity.OrderStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderResponse {
    
    private Long id;
    private Long marketId;
    private String marketName;
    private String marketAddress;
    private String marketPhone;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> orderItems;
    private BigDecimal totalAmount;
    private Long itemCount;
    
    // Constructors
    public OrderResponse() {}
    
    public OrderResponse(Long id, Long marketId, String marketName, String marketAddress, 
                        OrderStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.marketId = marketId;
        this.marketName = marketName;
        this.marketAddress = marketAddress;
        this.status = status;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getMarketId() {
        return marketId;
    }
    
    public void setMarketId(Long marketId) {
        this.marketId = marketId;
    }
    
    public String getMarketName() {
        return marketName;
    }
    
    public void setMarketName(String marketName) {
        this.marketName = marketName;
    }
    
    public String getMarketAddress() {
        return marketAddress;
    }
    
    public void setMarketAddress(String marketAddress) {
        this.marketAddress = marketAddress;
    }
    
    public String getMarketPhone() {
        return marketPhone;
    }
    
    @JsonProperty("marketPhone")
    public void setMarketPhone(String marketPhone) {
        this.marketPhone = marketPhone;
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public void setStatus(OrderStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public List<OrderItemResponse> getOrderItems() {
        return orderItems;
    }
    
    public void setOrderItems(List<OrderItemResponse> orderItems) {
        this.orderItems = orderItems;
    }
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public Long getItemCount() {
        return itemCount;
    }
    
    public void setItemCount(Long itemCount) {
        this.itemCount = itemCount;
    }
}
