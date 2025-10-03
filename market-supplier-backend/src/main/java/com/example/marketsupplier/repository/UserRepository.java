package com.example.marketsupplier.repository;

import com.example.marketsupplier.entity.User;
import com.example.marketsupplier.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Find user by email
    Optional<User> findByEmail(String email);
    
    // Check if email exists
    boolean existsByEmail(String email);
    
    // Find users by role
    List<User> findByRole(UserRole role);
    
    // Find users by name containing (case insensitive)
    @Query("SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<User> findByNameContainingIgnoreCase(@Param("name") String name);
    
    // Count users by role
    long countByRole(UserRole role);
    
    // Find active users (you can extend this based on your requirements)
    @Query("SELECT u FROM User u WHERE u.createdAt IS NOT NULL ORDER BY u.createdAt DESC")
    List<User> findAllOrderByCreatedAtDesc();
}
