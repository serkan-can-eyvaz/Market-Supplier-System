package com.example.marketsupplier.agent.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Item {
    private String raw;
    @JsonProperty("product_query")
    private String productQuery;
    @JsonProperty("product_id")
    private Long productId;
    private int qty;
    private String unit;

    // Getters and setters
    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }

    public String getProductQuery() {
        return productQuery;
    }

    public void setProductQuery(String productQuery) {
        this.productQuery = productQuery;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}
