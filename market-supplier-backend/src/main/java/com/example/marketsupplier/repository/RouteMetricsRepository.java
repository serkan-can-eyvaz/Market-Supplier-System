package com.example.marketsupplier.repository;

import com.example.marketsupplier.entity.RouteMetrics;
import com.example.marketsupplier.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteMetricsRepository extends JpaRepository<RouteMetrics, Long> {
    List<RouteMetrics> findTop20BySupplierOrderByCreatedAtDesc(Supplier supplier);
}


