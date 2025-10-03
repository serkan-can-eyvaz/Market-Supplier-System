package com.example.marketsupplier.dto;

import java.time.LocalDateTime;
import java.util.List;

public class AiUserPreferenceDto {
    private String phone;
    private String productName;
    private Long selectedProductId;
    private String queryType;
    private LocalDateTime timestamp;
    private List<Long> favoriteProducts;

    public AiUserPreferenceDto() {}

    public AiUserPreferenceDto(String phone, String productName, Long selectedProductId, String queryType) {
        this.phone = phone;
        this.productName = productName;
        this.selectedProductId = selectedProductId;
        this.queryType = queryType;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Long getSelectedProductId() {
        return selectedProductId;
    }

    public void setSelectedProductId(Long selectedProductId) {
        this.selectedProductId = selectedProductId;
    }

    public String getQueryType() {
        return queryType;
    }

    public void setQueryType(String queryType) {
        this.queryType = queryType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public List<Long> getFavoriteProducts() {
        return favoriteProducts;
    }

    public void setFavoriteProducts(List<Long> favoriteProducts) {
        this.favoriteProducts = favoriteProducts;
    }
}
