package com.example.marketsupplier.dto;

import java.time.LocalDateTime;

public class DeliveryEtaResponse {
    private Long orderId;
    private Long deliveryId;
    private LocalDateTime estimatedDeliveryTime;
    private Integer stopsAhead;
    private String status;
    private LocalDateTime plannedDate;

    public DeliveryEtaResponse(Long orderId, Long deliveryId, LocalDateTime estimatedDeliveryTime, Integer stopsAhead, String status, LocalDateTime plannedDate) {
        this.orderId = orderId;
        this.deliveryId = deliveryId;
        this.estimatedDeliveryTime = estimatedDeliveryTime;
        this.stopsAhead = stopsAhead;
        this.status = status;
        this.plannedDate = plannedDate;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getDeliveryId() { return deliveryId; }
    public void setDeliveryId(Long deliveryId) { this.deliveryId = deliveryId; }

    public LocalDateTime getEstimatedDeliveryTime() { return estimatedDeliveryTime; }
    public void setEstimatedDeliveryTime(LocalDateTime estimatedDeliveryTime) { this.estimatedDeliveryTime = estimatedDeliveryTime; }

    public Integer getStopsAhead() { return stopsAhead; }
    public void setStopsAhead(Integer stopsAhead) { this.stopsAhead = stopsAhead; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getPlannedDate() { return plannedDate; }
    public void setPlannedDate(LocalDateTime plannedDate) { this.plannedDate = plannedDate; }
}


