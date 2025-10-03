package com.example.marketsupplier.dto;

import jakarta.validation.constraints.NotBlank;

public class RoutePlanRequest {
    
    @NotBlank(message = "Route info is required")
    private String routeInfo;
    
    // Constructors
    public RoutePlanRequest() {}
    
    public RoutePlanRequest(String routeInfo) {
        this.routeInfo = routeInfo;
    }
    
    // Getters and Setters
    public String getRouteInfo() {
        return routeInfo;
    }
    
    public void setRouteInfo(String routeInfo) {
        this.routeInfo = routeInfo;
    }
}
