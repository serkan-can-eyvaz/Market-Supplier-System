package com.example.marketsupplier.repository;

import com.example.marketsupplier.entity.Delivery;
import com.example.marketsupplier.entity.DeliveryStatus;
import com.example.marketsupplier.entity.Order;
import com.example.marketsupplier.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    
    // Find delivery by order
    Optional<Delivery> findByOrder(Order order);
    
    // Find delivery by order id
    Optional<Delivery> findByOrderId(Long orderId);
    
    // Find deliveries by supplier
    List<Delivery> findBySupplier(Supplier supplier);
    
    // Find deliveries by supplier id
    List<Delivery> findBySupplierId(Long supplierId);
    
    // Find deliveries by supplier id ordered by creation date
    List<Delivery> findBySupplierIdOrderByCreatedAtDesc(Long supplierId);
    
    // Find deliveries by supplier id with pagination
    Page<Delivery> findBySupplierIdOrderByCreatedAtDesc(Long supplierId, Pageable pageable);
    
    // Find deliveries by status
    List<Delivery> findByDeliveryStatus(DeliveryStatus status);
    
    // Find deliveries by supplier and status
    List<Delivery> findBySupplierAndDeliveryStatus(Supplier supplier, DeliveryStatus status);
    
    List<Delivery> findBySupplierIdAndDeliveryStatus(Long supplierId, DeliveryStatus status);
    
    // Find deliveries created between dates
    List<Delivery> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // Find deliveries delivered between dates
    List<Delivery> findByDeliveryTimeBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // Find deliveries by supplier and created between dates
    List<Delivery> findBySupplierAndCreatedAtBetween(Supplier supplier, LocalDateTime startDate, LocalDateTime endDate);
    
    // Find deliveries by supplier and delivered between dates
    List<Delivery> findBySupplierAndDeliveryTimeBetween(Supplier supplier, LocalDateTime startDate, LocalDateTime endDate);
    
    // Find in-progress deliveries for a supplier
    List<Delivery> findBySupplierAndDeliveryStatusOrderByCreatedAtDesc(Supplier supplier, DeliveryStatus status);
    
    // Find completed deliveries for a supplier
    List<Delivery> findBySupplierAndDeliveryStatusOrderByDeliveryTimeDesc(Supplier supplier, DeliveryStatus status);
    
    // Count deliveries by status
    long countByDeliveryStatus(DeliveryStatus status);
    
    // Count deliveries by supplier
    long countBySupplier(Supplier supplier);
    
    long countBySupplierId(Long supplierId);
    
    // Count deliveries by supplier and status
    long countBySupplierAndDeliveryStatus(Supplier supplier, DeliveryStatus status);
    
    long countBySupplierIdAndDeliveryStatus(Long supplierId, DeliveryStatus status);
    
    // Find recent deliveries
    List<Delivery> findTop10ByOrderByCreatedAtDesc();
    
    // Find deliveries for today's report
    @Query("SELECT d FROM Delivery d WHERE d.supplier.id = :supplierId AND DATE(d.createdAt) = DATE(:date)")
    List<Delivery> findBySupplierIdAndDate(@Param("supplierId") Long supplierId, @Param("date") LocalDateTime date);
    
    // Find completed deliveries for daily report
    @Query("SELECT d FROM Delivery d WHERE d.supplier.id = :supplierId AND d.deliveryStatus = 'DELIVERED' AND DATE(d.deliveryTime) = DATE(:date)")
    List<Delivery> findCompletedDeliveriesBySupplierIdAndDate(@Param("supplierId") Long supplierId, @Param("date") LocalDateTime date);
    
    // Find deliveries with route info
    @Query("SELECT d FROM Delivery d WHERE d.routeInfo IS NOT NULL")
    List<Delivery> findDeliveriesWithRouteInfo();
    
    // Check if order has delivery
    boolean existsByOrder(Order order);
    
    boolean existsByOrderId(Long orderId);
}
