package com.example.marketsupplier.dto;

import java.time.LocalDateTime;

public class RouteMetricsResponse {
    private Long id;
    private Long supplierId;
    private Double totalDistanceKm;
    private Double totalDurationMin;
    private Integer stopsCount;
    private Double fuelConsumptionLPer100km;
    private Double fuelPriceTlPerL;
    private Double fuelEstimateLiters;
    private Double fuelCostEstimateTl;
    private LocalDateTime createdAt;

    public RouteMetricsResponse() {}

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public Double getTotalDistanceKm() { return totalDistanceKm; }
    public void setTotalDistanceKm(Double totalDistanceKm) { this.totalDistanceKm = totalDistanceKm; }
    public Double getTotalDurationMin() { return totalDurationMin; }
    public void setTotalDurationMin(Double totalDurationMin) { this.totalDurationMin = totalDurationMin; }
    public Integer getStopsCount() { return stopsCount; }
    public void setStopsCount(Integer stopsCount) { this.stopsCount = stopsCount; }
    public Double getFuelConsumptionLPer100km() { return fuelConsumptionLPer100km; }
    public void setFuelConsumptionLPer100km(Double fuelConsumptionLPer100km) { this.fuelConsumptionLPer100km = fuelConsumptionLPer100km; }
    public Double getFuelPriceTlPerL() { return fuelPriceTlPerL; }
    public void setFuelPriceTlPerL(Double fuelPriceTlPerL) { this.fuelPriceTlPerL = fuelPriceTlPerL; }
    public Double getFuelEstimateLiters() { return fuelEstimateLiters; }
    public void setFuelEstimateLiters(Double fuelEstimateLiters) { this.fuelEstimateLiters = fuelEstimateLiters; }
    public Double getFuelCostEstimateTl() { return fuelCostEstimateTl; }
    public void setFuelCostEstimateTl(Double fuelCostEstimateTl) { this.fuelCostEstimateTl = fuelCostEstimateTl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}


