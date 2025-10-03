package com.example.marketsupplier.repository;

import com.example.marketsupplier.entity.Supplier;
import com.example.marketsupplier.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    
    // Find supplier by user
    Optional<Supplier> findByUser(User user);
    
    // Find supplier by user id
    Optional<Supplier> findByUserId(Long userId);
    
    // Find suppliers by company name containing (case insensitive)
    @Query("SELECT s FROM Supplier s WHERE LOWER(s.companyName) LIKE LOWER(CONCAT('%', :companyName, '%'))")
    List<Supplier> findByCompanyNameContainingIgnoreCase(@Param("companyName") String companyName);
    
    // Find suppliers by phone
    Optional<Supplier> findByPhone(String phone);
    
    // Find supplier by WhatsApp phone number ID
    Optional<Supplier> findByPhoneNumberId(String phoneNumberId);
    
    // Check if supplier exists for user
    boolean existsByUser(User user);
    
    boolean existsByUserId(Long userId);
    
    // Get suppliers with their delivery counts
    @Query("SELECT s, COUNT(d) FROM Supplier s LEFT JOIN s.deliveries d GROUP BY s ORDER BY COUNT(d) DESC")
    List<Object[]> findSuppliersWithDeliveryCounts();
    
    // Find suppliers ordered by creation date
    List<Supplier> findAllByOrderByCreatedAtDesc();
    
    // Find suppliers by company name exact match
    Optional<Supplier> findByCompanyName(String companyName);
    
    // Check if company name exists
    boolean existsByCompanyName(String companyName);
}
