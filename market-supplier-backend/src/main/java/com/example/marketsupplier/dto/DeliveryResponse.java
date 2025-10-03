package com.example.marketsupplier.dto;

import com.example.marketsupplier.entity.DeliveryStatus;
import java.time.LocalDateTime;

public class DeliveryResponse {
    
    private Long id;
    private Long orderId;
    private String marketName;
    private String marketAddress;
    private Double marketLat;
    private Double marketLng;
    private Long supplierId;
    private String supplierCompanyName;
    private DeliveryStatus deliveryStatus;
    private LocalDateTime deliveryTime;
    private String routeInfo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public DeliveryResponse() {}
    
    public DeliveryResponse(Long id, Long orderId, String marketName, String marketAddress, 
                           Long supplierId, String supplierCompanyName, DeliveryStatus deliveryStatus, 
                           LocalDateTime createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.marketName = marketName;
        this.marketAddress = marketAddress;
        this.supplierId = supplierId;
        this.supplierCompanyName = supplierCompanyName;
        this.deliveryStatus = deliveryStatus;
        this.createdAt = createdAt;
    }
    
    public DeliveryResponse(Long id, Long orderId, String marketName, String marketAddress, 
                           Double marketLat, Double marketLng, Long supplierId, 
                           String supplierCompanyName, DeliveryStatus deliveryStatus, 
                           LocalDateTime createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.marketName = marketName;
        this.marketAddress = marketAddress;
        this.marketLat = marketLat;
        this.marketLng = marketLng;
        this.supplierId = supplierId;
        this.supplierCompanyName = supplierCompanyName;
        this.deliveryStatus = deliveryStatus;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getOrderId() {
        return orderId;
    }
    
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
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
    
    public Double getMarketLat() {
        return marketLat;
    }
    
    public void setMarketLat(Double marketLat) {
        this.marketLat = marketLat;
    }
    
    public Double getMarketLng() {
        return marketLng;
    }
    
    public void setMarketLng(Double marketLng) {
        this.marketLng = marketLng;
    }
    
    public Long getSupplierId() {
        return supplierId;
    }
    
    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }
    
    public String getSupplierCompanyName() {
        return supplierCompanyName;
    }
    
    public void setSupplierCompanyName(String supplierCompanyName) {
        this.supplierCompanyName = supplierCompanyName;
    }
    
    public DeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
    }
    
    public void setDeliveryStatus(DeliveryStatus deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }
    
    public LocalDateTime getDeliveryTime() {
        return deliveryTime;
    }
    
    public void setDeliveryTime(LocalDateTime deliveryTime) {
        this.deliveryTime = deliveryTime;
    }
    
    public String getRouteInfo() {
        return routeInfo;
    }
    
    public void setRouteInfo(String routeInfo) {
        this.routeInfo = routeInfo;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
