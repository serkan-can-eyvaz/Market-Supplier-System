package com.example.marketsupplier.repository;

import com.example.marketsupplier.entity.CartItem;
import com.example.marketsupplier.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.market.id = :marketId")
    List<CartItem> findByMarketId(@Param("marketId") Long marketId);
    
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId")
    List<CartItem> findByCartId(@Param("cartId") Long cartId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id IN (SELECT c.id FROM Cart c WHERE c.market.id = :marketId)")
    void deleteByMarketId(@Param("marketId") Long marketId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
    void deleteByCartId(@Param("cartId") Long cartId);
    
    // Standard JPA methods
    List<CartItem> findByCart(Cart cart);
    Optional<CartItem> findByCartAndProductId(Cart cart, Long productId);
    Optional<CartItem> findByIdAndCart(Long id, Cart cart);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem ci WHERE ci.cart = ?1")
    void deleteByCart(Cart cart);
    
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM cart_items WHERE cart_id = ?1", nativeQuery = true)
    void deleteByCartIdNative(Long cartId);

    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId AND LOWER(ci.productName) = LOWER(:name)")
    void deleteByCartIdAndName(@Param("cartId") Long cartId, @Param("name") String name);
}