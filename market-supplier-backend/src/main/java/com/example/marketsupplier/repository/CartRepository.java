package com.example.marketsupplier.repository;

import com.example.marketsupplier.entity.Cart;
import com.example.marketsupplier.entity.Market;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    
    @Query("SELECT c FROM Cart c WHERE c.market = :market ORDER BY c.createdAt DESC")
    Optional<Cart> findLatestByMarket(@Param("market") Market market);
    
    @Query("SELECT c FROM Cart c WHERE c.market.id = :marketId ORDER BY c.createdAt DESC")
    Optional<Cart> findLatestByMarketId(@Param("marketId") Long marketId);
    
    // Eksik method
    Optional<Cart> findByMarket(Market market);
}