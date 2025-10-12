package com.example.marketsupplier.service;

import com.example.marketsupplier.entity.*;
import com.example.marketsupplier.repository.DeliveryRepository;
import com.example.marketsupplier.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DeliveryService {
    
    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);
    
    @Autowired
    private DeliveryRepository deliveryRepository;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private SupplierService supplierService;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private NotificationService notificationService;
    
    // Create delivery assignment
    public Delivery createDelivery(Long orderId, Long supplierId) {
        // Get order
        Order order = orderService.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        
        // Get supplier
        Supplier supplier = supplierService.findById(supplierId)
            .orElseThrow(() -> new RuntimeException("Supplier not found with id: " + supplierId));
        
        // Check if order already has delivery
        if (deliveryRepository.existsByOrder(order)) {
            throw new RuntimeException("Order already has a delivery assignment");
        }
        
        // Previously only APPROVED/DELIVERED; allow PENDING as well to plan early
        
        // Create delivery
        Delivery delivery = new Delivery(order, supplier);
        Delivery savedDelivery = deliveryRepository.save(delivery);
        
        // Tedarikçiye bildirim gönder
        try {
            User supplierUser = supplier.getUser();
            notificationService.notifyNewDelivery(supplierUser, order.getMarket().getName(), savedDelivery.getId());
            log.info("Notification created for new delivery: {} to market: {}", savedDelivery.getId(), order.getMarket().getName());
        } catch (Exception e) {
            log.error("Failed to create notification for delivery '{}': {}", savedDelivery.getId(), e.getMessage());
        }
        
        return savedDelivery;
    }

    // Create deliveries for all APPROVED orders without delivery for a supplier
    public int createDeliveriesForApprovedWithoutDelivery(Long supplierId) {
        Supplier supplier = supplierService.findById(supplierId)
            .orElseThrow(() -> new RuntimeException("Supplier not found with id: " + supplierId));
        int created = 0;
        for (Order order : orderRepository.findOrdersWithoutDelivery()) {
            if (order.getStatus() == OrderStatus.APPROVED) {
                try {
                    Delivery delivery = new Delivery(order, supplier);
                    deliveryRepository.save(delivery);
                    created++;
                } catch (Exception ignored) {}
            }
        }
        return created;
    }
    
    // Plan delivery route
    public Delivery planDeliveryRoute(Long deliveryId, String routeInfo) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
            .orElseThrow(() -> new RuntimeException("Delivery not found with id: " + deliveryId));
        
        // Check if delivery is in progress
        if (delivery.getDeliveryStatus() != DeliveryStatus.IN_PROGRESS) {
            throw new RuntimeException("Can only plan route for in-progress deliveries");
        }
        
        delivery.setRouteInfo(routeInfo);
        return deliveryRepository.save(delivery);
    }
    
    // Complete delivery
    public Delivery completeDelivery(Long deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
            .orElseThrow(() -> new RuntimeException("Delivery not found with id: " + deliveryId));
        
        // Check if delivery is in progress
        if (delivery.getDeliveryStatus() != DeliveryStatus.IN_PROGRESS) {
            throw new RuntimeException("Delivery is already completed");
        }
        
        // Mark delivery as completed
        delivery.markAsDelivered();
        
        // Update order status
        Order order = delivery.getOrder();
        order.setStatus(OrderStatus.DELIVERED);
        // OrderService.completeOrder yalnizca PENDING icin izin veriyor olabilir; burada direkt kaydediyoruz
        orderRepository.save(order);
        
        Delivery savedDelivery = deliveryRepository.save(delivery);
        
        // Market sahibine teslimat tamamlandı bildirimi gönder
        try {
            User marketUser = order.getMarket().getUser();
            notificationService.notifyDeliveryStatusChange(marketUser, "DELIVERED", order.getMarket().getName(), savedDelivery.getId());
            log.info("Notification created for delivery completion: {} to market: {}", savedDelivery.getId(), order.getMarket().getName());
        } catch (Exception e) {
            log.error("Failed to create notification for delivery completion '{}': {}", savedDelivery.getId(), e.getMessage());
        }
        
        return savedDelivery;
    }
    
    // Find delivery by ID
    public Optional<Delivery> findById(Long id) {
        return deliveryRepository.findById(id);
    }
    
    // Find delivery by order
    public Optional<Delivery> findByOrder(Long orderId) {
        return deliveryRepository.findByOrderId(orderId);
    }
    
    // Get deliveries by supplier
    public List<Delivery> getDeliveriesBySupplier(Long supplierId) {
        return deliveryRepository.findBySupplierIdOrderByCreatedAtDesc(supplierId);
    }
    
    // Get deliveries by supplier with pagination
    public Page<Delivery> getDeliveriesBySupplierPaginated(Long supplierId, Pageable pageable) {
        return deliveryRepository.findBySupplierIdOrderByCreatedAtDesc(supplierId, pageable);
    }
    
    // Get all deliveries with pagination (Admin only)
    public Page<Delivery> getAllDeliveriesPaginated(Pageable pageable) {
        return deliveryRepository.findAll(pageable);
    }
    
    // Get deliveries by supplier and status
    public List<Delivery> getDeliveriesBySupplierAndStatus(Long supplierId, DeliveryStatus status) {
        return deliveryRepository.findBySupplierIdAndDeliveryStatus(supplierId, status);
    }
    
    // Get in-progress deliveries for supplier
    public List<Delivery> getInProgressDeliveriesForSupplier(Long supplierId) {
        return deliveryRepository.findBySupplierAndDeliveryStatusOrderByCreatedAtDesc(
            supplierService.findById(supplierId).orElseThrow(() -> new RuntimeException("Supplier not found")),
            DeliveryStatus.IN_PROGRESS
        );
    }
    
    // Get completed deliveries for supplier
    public List<Delivery> getCompletedDeliveriesForSupplier(Long supplierId) {
        return deliveryRepository.findBySupplierAndDeliveryStatusOrderByDeliveryTimeDesc(
            supplierService.findById(supplierId).orElseThrow(() -> new RuntimeException("Supplier not found")),
            DeliveryStatus.DELIVERED
        );
    }
    
    // Get deliveries by date range
    public List<Delivery> getDeliveriesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return deliveryRepository.findByCreatedAtBetween(startDate, endDate);
    }
    
    // Get deliveries by supplier and date range
    public List<Delivery> getDeliveriesBySupplierAndDateRange(Long supplierId, LocalDateTime startDate, LocalDateTime endDate) {
        return deliveryRepository.findBySupplierAndCreatedAtBetween(
            supplierService.findById(supplierId).orElseThrow(() -> new RuntimeException("Supplier not found")),
            startDate, endDate
        );
    }
    
    // Get daily deliveries for supplier
    public List<Delivery> getDailyDeliveriesForSupplier(Long supplierId, LocalDateTime date) {
        return deliveryRepository.findBySupplierIdAndDate(supplierId, date);
    }
    
    // Get completed deliveries for daily report
    public List<Delivery> getCompletedDeliveriesForDailyReport(Long supplierId, LocalDateTime date) {
        return deliveryRepository.findCompletedDeliveriesBySupplierIdAndDate(supplierId, date);
    }
    
    // Get deliveries with route info
    public List<Delivery> getDeliveriesWithRouteInfo() {
        return deliveryRepository.findDeliveriesWithRouteInfo();
    }
    
    // Get delivery statistics
    public DeliveryStats getDeliveryStats() {
        long totalDeliveries = deliveryRepository.count();
        long inProgressDeliveries = deliveryRepository.countByDeliveryStatus(DeliveryStatus.IN_PROGRESS);
        long completedDeliveries = deliveryRepository.countByDeliveryStatus(DeliveryStatus.DELIVERED);
        
        return new DeliveryStats(totalDeliveries, inProgressDeliveries, completedDeliveries);
    }
    
    // Get delivery statistics for supplier
    public DeliveryStats getDeliveryStatsForSupplier(Long supplierId) {
        Supplier supplier = supplierService.findById(supplierId)
            .orElseThrow(() -> new RuntimeException("Supplier not found with id: " + supplierId));
        
        long totalDeliveries = deliveryRepository.countBySupplier(supplier);
        long inProgressDeliveries = deliveryRepository.countBySupplierAndDeliveryStatus(supplier, DeliveryStatus.IN_PROGRESS);
        long completedDeliveries = deliveryRepository.countBySupplierAndDeliveryStatus(supplier, DeliveryStatus.DELIVERED);
        
        return new DeliveryStats(totalDeliveries, inProgressDeliveries, completedDeliveries);
    }
    
    // Validate delivery ownership
    public boolean isDeliveryOwner(Long deliveryId, Long supplierId) {
        Optional<Delivery> delivery = deliveryRepository.findById(deliveryId);
        return delivery.isPresent() && delivery.get().getSupplier().getId().equals(supplierId);
    }
    
    // Check if order has delivery
    public boolean orderHasDelivery(Long orderId) {
        return deliveryRepository.existsByOrderId(orderId);
    }
    
    // Get recent deliveries
    public List<Delivery> getRecentDeliveries() {
        return deliveryRepository.findTop10ByOrderByCreatedAtDesc();
    }
    
    // Inner class for delivery statistics
    public static class DeliveryStats {
        private final long totalDeliveries;
        private final long inProgressDeliveries;
        private final long completedDeliveries;
        
        public DeliveryStats(long totalDeliveries, long inProgressDeliveries, long completedDeliveries) {
            this.totalDeliveries = totalDeliveries;
            this.inProgressDeliveries = inProgressDeliveries;
            this.completedDeliveries = completedDeliveries;
        }
        
        // Getters
        public long getTotalDeliveries() { return totalDeliveries; }
        public long getInProgressDeliveries() { return inProgressDeliveries; }
        public long getCompletedDeliveries() { return completedDeliveries; }
    }
}
