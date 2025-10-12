package com.example.marketsupplier.repository;

import com.example.marketsupplier.entity.Product;
import com.example.marketsupplier.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    // Basic queries
    List<Product> findBySupplierAndIsActiveTrueOrderByNameAsc(Supplier supplier);
    
    List<Product> findBySupplierOrderByNameAsc(Supplier supplier);
    
    Page<Product> findBySupplierOrderByNameAsc(Supplier supplier, Pageable pageable);
    
    Optional<Product> findByIdAndSupplier(Long id, Supplier supplier);
    
    @Query("SELECT p FROM Product p WHERE p.supplier = :supplier AND p.isActive = true AND LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY p.name ASC")
    List<Product> findBySupplierAndNameContainingIgnoreCase(@Param("supplier") Supplier supplier, @Param("searchTerm") String searchTerm);
    
    boolean existsBySupplierAndNameIgnoreCase(Supplier supplier, String name);
    
    boolean existsBySupplierAndNameIgnoreCaseAndIdNot(Supplier supplier, String name, Long id);
    
    List<Product> findByIsActiveTrueOrderByNameAsc();

    // Performance optimized queries
    @Query("SELECT p FROM Product p WHERE p.supplier.id = :supplierId AND p.isActive = true ORDER BY p.name ASC")
    List<Product> findBySupplierIdAndIsActiveTrueOrderByNameAsc(@Param("supplierId") Long supplierId);
    
    @Query("SELECT p FROM Product p WHERE p.supplier.id = :supplierId AND p.isActive = true ORDER BY p.name ASC")
    Page<Product> findBySupplierIdAndIsActiveTrueOrderByNameAsc(@Param("supplierId") Long supplierId, Pageable pageable);

    // Tüm ürünleri getir (aktif + pasif) - tedarikçi için
    @Query("SELECT p FROM Product p WHERE p.supplier.id = :supplierId ORDER BY p.name ASC")
    Page<Product> findBySupplierIdOrderByNameAsc(@Param("supplierId") Long supplierId, Pageable pageable);

    // Sadece pasif ürünleri getir - tedarikçi için
    @Query("SELECT p FROM Product p WHERE p.supplier.id = :supplierId AND p.isActive = false ORDER BY p.name ASC")
    Page<Product> findBySupplierIdAndIsActiveFalseOrderByNameAsc(@Param("supplierId") Long supplierId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.supplier.id IN :supplierIds AND p.isActive = true ORDER BY p.supplier.id, p.name ASC")
    List<Product> findBySupplierIdInAndIsActiveTrueOrderBySupplierIdAscNameAsc(@Param("supplierIds") Set<Long> supplierIds);

    @Query("SELECT p.id, p.name, p.price, p.stockQuantity FROM Product p WHERE p.supplier.id = :supplierId AND p.isActive = true ORDER BY p.name ASC")
    List<Object[]> findProductSummaryBySupplierId(@Param("supplierId") Long supplierId);

    @Query("SELECT p FROM Product p WHERE p.supplier.id = :supplierId AND p.isActive = true AND LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY p.name ASC")
    List<Product> findBySupplierIdAndNameContainingIgnoreCase(@Param("supplierId") Long supplierId, @Param("searchTerm") String searchTerm);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.supplier.id = :supplierId AND p.isActive = true")
    long countBySupplierIdAndIsActiveTrue(@Param("supplierId") Long supplierId);

    @Query("SELECT p FROM Product p WHERE p.supplier.id = :supplierId AND p.isActive = true AND p.stockQuantity > 0 ORDER BY p.name ASC")
    List<Product> findAvailableProductsBySupplierId(@Param("supplierId") Long supplierId);

    @Query("SELECT p FROM Product p WHERE p.supplier.id = :supplierId AND p.isActive = true AND p.stockQuantity <= :lowStockThreshold ORDER BY p.stockQuantity ASC")
    List<Product> findLowStockProductsBySupplierId(@Param("supplierId") Long supplierId, @Param("lowStockThreshold") int lowStockThreshold);

    // Batch operations
    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :quantity WHERE p.id = :productId AND p.stockQuantity >= :quantity")
    int updateStockById(@Param("productId") Long productId, @Param("quantity") int quantity);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity + :quantity WHERE p.id = :productId")
    int addStockById(@Param("productId") Long productId, @Param("quantity") int quantity);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.isActive = false WHERE p.supplier.id = :supplierId")
    int deactivateProductsBySupplierId(@Param("supplierId") Long supplierId);

    // Bulk operations
    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :quantity WHERE p.id IN :productIds AND p.stockQuantity >= :quantity")
    int updateStockByIds(@Param("productIds") Set<Long> productIds, @Param("quantity") int quantity);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.price = :newPrice WHERE p.supplier.id = :supplierId AND p.isActive = true")
    int updatePriceBySupplierId(@Param("supplierId") Long supplierId, @Param("newPrice") java.math.BigDecimal newPrice);

    // Search optimization
    @Query("SELECT p FROM Product p WHERE p.supplier.id = :supplierId AND p.isActive = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY p.name ASC")
    List<Product> searchProductsBySupplierId(@Param("supplierId") Long supplierId, @Param("searchTerm") String searchTerm);

    // Statistics queries
    @Query("SELECT p.supplier.id, COUNT(p), AVG(p.price), SUM(p.stockQuantity) FROM Product p WHERE p.isActive = true GROUP BY p.supplier.id")
    List<Object[]> getProductStatisticsBySupplier();

    @Query("SELECT COUNT(p), AVG(p.price), SUM(p.stockQuantity) FROM Product p WHERE p.supplier.id = :supplierId AND p.isActive = true")
    Object[] getProductStatisticsBySupplierId(@Param("supplierId") Long supplierId);

    // Top products
    @Query("SELECT p FROM Product p WHERE p.supplier.id = :supplierId AND p.isActive = true ORDER BY p.stockQuantity DESC")
    List<Product> findTopProductsByStock(@Param("supplierId") Long supplierId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.supplier.id = :supplierId AND p.isActive = true ORDER BY p.price ASC")
    List<Product> findCheapestProducts(@Param("supplierId") Long supplierId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.supplier.id = :supplierId AND p.isActive = true ORDER BY p.price DESC")
    List<Product> findMostExpensiveProducts(@Param("supplierId") Long supplierId, Pageable pageable);
}
