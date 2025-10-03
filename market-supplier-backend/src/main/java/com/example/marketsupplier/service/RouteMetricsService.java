package com.example.marketsupplier.service;

import com.example.marketsupplier.dto.RouteMetricsRequest;
import com.example.marketsupplier.dto.RouteMetricsResponse;
import com.example.marketsupplier.entity.RouteMetrics;
import com.example.marketsupplier.entity.Supplier;
import com.example.marketsupplier.repository.RouteMetricsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RouteMetricsService {

    @Autowired
    private RouteMetricsRepository routeMetricsRepository;

    @Autowired
    private SupplierService supplierService;

    private static final double DEFAULT_FUEL_L_PER_100KM = 8.5; // varsayılan tüketim
    private static final double DEFAULT_FUEL_PRICE_TL_PER_L = 45.0; // varsayılan pompa fiyatı

    public RouteMetricsResponse saveMetrics(RouteMetricsRequest req) {
        Supplier supplier = supplierService.findById(req.getSupplierId())
            .orElseThrow(() -> new RuntimeException("Supplier not found"));

        RouteMetrics m = new RouteMetrics(supplier);
        m.setTotalDistanceKm(req.getTotalDistanceKm());
        m.setTotalDurationMin(req.getTotalDurationMin());
        m.setStopsCount(req.getStopsCount());

        double cons = req.getFuelConsumptionLPer100km() != null ? req.getFuelConsumptionLPer100km() : DEFAULT_FUEL_L_PER_100KM;
        double price = req.getFuelPriceTlPerL() != null ? req.getFuelPriceTlPerL() : DEFAULT_FUEL_PRICE_TL_PER_L;
        m.setFuelConsumptionLPer100km(cons);
        m.setFuelPriceTlPerL(price);

        double liters = (req.getTotalDistanceKm() != null ? req.getTotalDistanceKm() : 0.0) * cons / 100.0;
        m.setFuelEstimateLiters(liters);
        m.setFuelCostEstimateTl(liters * price);

        RouteMetrics saved = routeMetricsRepository.save(m);
        return toResponse(saved);
    }

    public List<RouteMetricsResponse> recentForSupplier(Long supplierId) {
        Supplier supplier = supplierService.findById(supplierId)
            .orElseThrow(() -> new RuntimeException("Supplier not found"));
        return routeMetricsRepository.findTop20BySupplierOrderByCreatedAtDesc(supplier)
            .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private RouteMetricsResponse toResponse(RouteMetrics m) {
        RouteMetricsResponse r = new RouteMetricsResponse();
        r.setId(m.getId());
        r.setSupplierId(m.getSupplier().getId());
        r.setTotalDistanceKm(m.getTotalDistanceKm());
        r.setTotalDurationMin(m.getTotalDurationMin());
        r.setStopsCount(m.getStopsCount());
        r.setFuelConsumptionLPer100km(m.getFuelConsumptionLPer100km());
        r.setFuelPriceTlPerL(m.getFuelPriceTlPerL());
        r.setFuelEstimateLiters(m.getFuelEstimateLiters());
        r.setFuelCostEstimateTl(m.getFuelCostEstimateTl());
        r.setCreatedAt(m.getCreatedAt());
        return r;
    }
}


