package com.example.marketsupplier.service;

import com.example.marketsupplier.entity.RoutePlanSnapshot;
import com.example.marketsupplier.repository.RoutePlanSnapshotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RoutePlanService {

    @Autowired
    private RoutePlanSnapshotRepository repository;

    @Autowired
    private SupplierService supplierService;

    public RoutePlanSnapshot savePlan(Long supplierId, String planJson) {
        var supplier = supplierService.findById(supplierId)
            .orElseThrow(() -> new RuntimeException("Supplier not found"));
        // Previous active plan -> deactivate
        repository.findTopBySupplierAndActiveOrderByCreatedAtDesc(supplier, true)
            .ifPresent(p -> { p.setActive(false); repository.save(p); });
        RoutePlanSnapshot snap = new RoutePlanSnapshot();
        snap.setSupplier(supplier);
        snap.setPlanJson(planJson);
        return repository.save(snap);
    }

    public RoutePlanSnapshot getActivePlan(Long supplierId) {
        var supplier = supplierService.findById(supplierId)
            .orElseThrow(() -> new RuntimeException("Supplier not found"));
        return repository.findTopBySupplierAndActiveOrderByCreatedAtDesc(supplier, true).orElse(null);
    }
}


