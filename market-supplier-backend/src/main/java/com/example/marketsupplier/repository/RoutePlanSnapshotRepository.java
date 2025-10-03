package com.example.marketsupplier.repository;

import com.example.marketsupplier.entity.RoutePlanSnapshot;
import com.example.marketsupplier.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoutePlanSnapshotRepository extends JpaRepository<RoutePlanSnapshot, Long> {
    Optional<RoutePlanSnapshot> findTopBySupplierAndActiveOrderByCreatedAtDesc(Supplier supplier, boolean active);
}


