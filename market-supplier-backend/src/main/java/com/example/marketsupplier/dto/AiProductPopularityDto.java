package com.example.marketsupplier.dto;

public class AiProductPopularityDto {
    private Long productId;
    private String productName;
    private Double popularityScore;
    private Integer orderCount;
    private Integer searchCount;

    public AiProductPopularityDto() {}

    public AiProductPopularityDto(Long productId, String productName, Double popularityScore, Integer orderCount, Integer searchCount) {
        this.productId = productId;
        this.productName = productName;
        this.popularityScore = popularityScore;
        this.orderCount = orderCount;
        this.searchCount = searchCount;
    }

    // Getters and Setters
    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Double getPopularityScore() {
        return popularityScore;
    }

    public void setPopularityScore(Double popularityScore) {
        this.popularityScore = popularityScore;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }

    public Integer getSearchCount() {
        return searchCount;
    }

    public void setSearchCount(Integer searchCount) {
        this.searchCount = searchCount;
    }
}
