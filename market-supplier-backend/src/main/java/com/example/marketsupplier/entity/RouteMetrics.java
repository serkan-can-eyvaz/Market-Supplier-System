package com.example.marketsupplier.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "route_metrics")
public class RouteMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "total_distance_km")
    private Double totalDistanceKm;

    @Column(name = "total_duration_min")
    private Double totalDurationMin;

    @Column(name = "stops_count")
    private Integer stopsCount;

    @Column(name = "fuel_consumption_l_per_100km")
    private Double fuelConsumptionLPer100km;

    @Column(name = "fuel_price_tl_per_l")
    private Double fuelPriceTlPerL;

    @Column(name = "fuel_estimate_liters")
    private Double fuelEstimateLiters;

    @Column(name = "fuel_cost_estimate_tl")
    private Double fuelCostEstimateTl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public RouteMetrics() {}

    public RouteMetrics(Supplier supplier) {
        this.supplier = supplier;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }

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


