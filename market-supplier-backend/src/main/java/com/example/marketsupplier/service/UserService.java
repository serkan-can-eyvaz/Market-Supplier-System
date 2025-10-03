package com.example.marketsupplier.service;

import com.example.marketsupplier.entity.User;
import com.example.marketsupplier.entity.UserRole;
import com.example.marketsupplier.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }
    
    // Create new user
    public User createUser(String name, String email, String password, UserRole role) {
        // Check if email already exists
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists: " + email);
        }
        
        // Encode password
        String encodedPassword = passwordEncoder.encode(password);
        
        // Create user
        User user = new User(name, email, encodedPassword, role);
        return userRepository.save(user);
    }
    
    // Find user by ID
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    
    // Find user by email
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    // Get all users
    public List<User> getAllUsers() {
        return userRepository.findAllOrderByCreatedAtDesc();
    }
    
    // Get users by role
    public List<User> getUsersByRole(UserRole role) {
        return userRepository.findByRole(role);
    }
    
    // Search users by name
    public List<User> searchUsersByName(String name) {
        return userRepository.findByNameContainingIgnoreCase(name);
    }
    
    // Update user
    public User updateUser(Long id, String name, String email) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        
        // Check if email is being changed and if new email already exists
        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists: " + email);
        }
        
        user.setName(name);
        user.setEmail(email);
        
        return userRepository.save(user);
    }
    
    // Update password
    public User updatePassword(Long id, String currentPassword, String newPassword) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        
        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        
        // Encode and set new password
        String encodedNewPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedNewPassword);
        
        return userRepository.save(user);
    }
    
    // Delete user
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
    
    // Check if email exists
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }
    
    // Get user count by role
    public long getUserCountByRole(UserRole role) {
        return userRepository.countByRole(role);
    }
    
    // Validate password
    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
    
    // Get user statistics
    public UserStats getUserStats() {
        long totalUsers = userRepository.count();
        long marketUsers = userRepository.countByRole(UserRole.MARKET);
        long supplierUsers = userRepository.countByRole(UserRole.SUPPLIER);
        long adminUsers = userRepository.countByRole(UserRole.ADMIN);
        
        return new UserStats(totalUsers, marketUsers, supplierUsers, adminUsers);
    }
    
    // Inner class for user statistics
    public static class UserStats {
        private final long totalUsers;
        private final long marketUsers;
        private final long supplierUsers;
        private final long adminUsers;
        
        public UserStats(long totalUsers, long marketUsers, long supplierUsers, long adminUsers) {
            this.totalUsers = totalUsers;
            this.marketUsers = marketUsers;
            this.supplierUsers = supplierUsers;
            this.adminUsers = adminUsers;
        }
        
        // Getters
        public long getTotalUsers() { return totalUsers; }
        public long getMarketUsers() { return marketUsers; }
        public long getSupplierUsers() { return supplierUsers; }
        public long getAdminUsers() { return adminUsers; }
    }
}
