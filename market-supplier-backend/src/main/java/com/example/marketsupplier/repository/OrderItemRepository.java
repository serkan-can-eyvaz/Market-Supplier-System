package com.example.marketsupplier.repository;

import com.example.marketsupplier.entity.Order;
import com.example.marketsupplier.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    
    // Find order items by order
    List<OrderItem> findByOrder(Order order);
    
    // Find order items by order id
    List<OrderItem> findByOrderId(Long orderId);
    
    // Find order items by product name containing (case insensitive)
    @Query("SELECT oi FROM OrderItem oi WHERE LOWER(oi.productName) LIKE LOWER(CONCAT('%', :productName, '%'))")
    List<OrderItem> findByProductNameContainingIgnoreCase(@Param("productName") String productName);
    
    // Find order items by exact product name
    List<OrderItem> findByProductName(String productName);
    
    // Find order items by unit
    List<OrderItem> findByUnit(String unit);
    
    // Find order items with price greater than
    List<OrderItem> findByPriceGreaterThan(BigDecimal price);
    
    // Find order items with price between
    List<OrderItem> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
    
    // Find order items with quantity greater than
    List<OrderItem> findByQuantityGreaterThan(Integer quantity);
    
    // Calculate total amount for an order
    @Query("SELECT SUM(oi.price * oi.quantity) FROM OrderItem oi WHERE oi.order.id = :orderId")
    BigDecimal calculateTotalAmountByOrderId(@Param("orderId") Long orderId);
    
    // Count items in an order
    long countByOrder(Order order);
    
    long countByOrderId(Long orderId);
    
    // Find most ordered products
    @Query("SELECT oi.productName, SUM(oi.quantity) FROM OrderItem oi GROUP BY oi.productName ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> findMostOrderedProducts();
    
    // Find order items by order and product name
    List<OrderItem> findByOrderAndProductName(Order order, String productName);
    
    // Delete order items by order
    void deleteByOrder(Order order);
    
    void deleteByOrderId(Long orderId);
    
    // Find average price by product name
    @Query("SELECT AVG(oi.price) FROM OrderItem oi WHERE oi.productName = :productName")
    BigDecimal findAveragePriceByProductName(@Param("productName") String productName);
}
