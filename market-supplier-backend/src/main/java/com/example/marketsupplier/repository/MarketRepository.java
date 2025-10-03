package com.example.marketsupplier.repository;

import com.example.marketsupplier.entity.Market;
import com.example.marketsupplier.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarketRepository extends JpaRepository<Market, Long> {
    
    // Find market by user
    Optional<Market> findByUser(User user);
    
    // Find market by user id
    Optional<Market> findByUserId(Long userId);

    // Find all markets by user id
    List<Market> findAllByUserId(Long userId);
    
    // Find all markets by user id with pagination
    Page<Market> findAllByUserId(Long userId, Pageable pageable);
    
    // Find markets by name containing (case insensitive)
    @Query("SELECT m FROM Market m WHERE LOWER(m.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Market> findByNameContainingIgnoreCase(@Param("name") String name);
    
    // Find markets by address containing (case insensitive)
    @Query("SELECT m FROM Market m WHERE LOWER(m.address) LIKE LOWER(CONCAT('%', :address, '%'))")
    List<Market> findByAddressContainingIgnoreCase(@Param("address") String address);
    
    // Find markets by phone
    Optional<Market> findByPhone(String phone);
    
    // Check if market exists for user
    boolean existsByUser(User user);
    
    boolean existsByUserId(Long userId);
    
    // Get markets with their order counts
    @Query("SELECT m, COUNT(o) FROM Market m LEFT JOIN m.orders o GROUP BY m ORDER BY COUNT(o) DESC")
    List<Object[]> findMarketsWithOrderCounts();
    
    // Find markets ordered by creation date
    List<Market> findAllByOrderByCreatedAtDesc();
}
