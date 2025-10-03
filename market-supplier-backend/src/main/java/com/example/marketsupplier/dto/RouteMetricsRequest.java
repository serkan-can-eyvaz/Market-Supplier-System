package com.example.marketsupplier.dto;

public class RouteMetricsRequest {
    private Long supplierId;
    private Double totalDistanceKm;
    private Double totalDurationMin;
    private Integer stopsCount;
    private Double fuelConsumptionLPer100km; // optional override
    private Double fuelPriceTlPerL;          // optional override

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
}


