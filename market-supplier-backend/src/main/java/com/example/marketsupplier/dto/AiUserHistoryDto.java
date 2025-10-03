package com.example.marketsupplier.dto;

import java.time.LocalDateTime;

public class AiUserHistoryDto {
    private String productName;
    private Integer quantity;
    private String unit;
    private Double price;
    private LocalDateTime orderDate;

    public AiUserHistoryDto() {}

    public AiUserHistoryDto(String productName, Integer quantity, String unit, Double price, LocalDateTime orderDate) {
        this.productName = productName;
        this.quantity = quantity;
        this.unit = unit;
        this.price = price;
        this.orderDate = orderDate;
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

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }
}
