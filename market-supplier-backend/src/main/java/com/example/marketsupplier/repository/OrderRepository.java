package com.example.marketsupplier.repository;

import com.example.marketsupplier.entity.Order;
import com.example.marketsupplier.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import com.example.marketsupplier.entity.Market;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    @Query("SELECT o FROM Order o WHERE o.market.id = :marketId ORDER BY o.createdAt DESC")
    List<Order> findByMarketIdOrderByCreatedAtDesc(@Param("marketId") Long marketId);
    
    @Query("SELECT o FROM Order o WHERE o.market.id = :marketId AND o.status = :status ORDER BY o.createdAt DESC")
    List<Order> findByMarketIdAndStatusOrderByCreatedAtDesc(@Param("marketId") Long marketId, @Param("status") OrderStatus status);
    
    @Query("SELECT o FROM Order o WHERE o.market.id = :marketId AND o.createdAt >= :since ORDER BY o.createdAt DESC")
    List<Order> findByMarketIdAndCreatedAtAfterOrderByCreatedAtDesc(@Param("marketId") Long marketId, @Param("since") LocalDateTime since);
    
    // Eksik method'lar
    List<Order> findByMarketIdAndStatus(Long marketId, OrderStatus status);
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);
    Page<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);
    
    @Query("SELECT o FROM Order o WHERE o.delivery IS NULL")
    List<Order> findOrdersWithoutDelivery();
    
    List<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    List<Order> findByMarketAndCreatedAtBetween(Market market, LocalDateTime start, LocalDateTime end);
    
    long countByStatus(OrderStatus status);
    
    @Query("SELECT o.id, o.totalPrice FROM Order o")
    List<Object[]> findOrdersWithTotalAmounts();
    
    long countByMarket(Market market);
    long countByMarketIdAndStatus(Long marketId, OrderStatus status);
    
    @Query("SELECT o FROM Order o WHERE o.market.id = :marketId AND o.status IN :statuses AND o.delivery IS NOT NULL ORDER BY o.createdAt DESC")
    List<Order> findLatestActiveOrderByMarketWithDelivery(@Param("marketId") Long marketId, @Param("statuses") List<OrderStatus> statuses);
    
    List<Order> findByMarketIdAndStatusInOrderByCreatedAtDesc(Long marketId, List<OrderStatus> statuses);
    
    // Teslim edilen siparişleri getir (en son teslim edilen)
    @Query("SELECT o FROM Order o WHERE o.market.id = :marketId AND o.status = :status ORDER BY o.createdAt DESC")
    List<Order> findDeliveredOrdersByMarket(@Param("marketId") Long marketId, @Param("status") OrderStatus status);
    
    // Onaylanmış ama teslim edilmemiş siparişleri getir
    @Query("SELECT o FROM Order o WHERE o.market.id = :marketId AND o.status IN :statuses ORDER BY o.createdAt DESC")
    List<Order> findApprovedButNotDeliveredOrders(@Param("marketId") Long marketId, @Param("statuses") List<OrderStatus> statuses);
    
    // AI Service için - Order entity'sinde productName field'ı yok
    
    // Sipariş verilen toplam ürün (kalem) sayısını getir
    @Query("SELECT COUNT(oi) FROM OrderItem oi")
    Long countTotalOrderItems();
}