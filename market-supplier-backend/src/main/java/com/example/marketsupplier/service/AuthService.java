package com.example.marketsupplier.service;

import com.example.marketsupplier.entity.User;
import com.example.marketsupplier.entity.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    // Register new user
    public AuthResponse register(String name, String email, String password, UserRole role) {
        try {
            // Default role to SUPPLIER when not provided
            if (role == null) {
                role = UserRole.SUPPLIER;
            }
            // Create user
            User user = userService.createUser(name, email, password, role);
            
            return new AuthResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
            
        } catch (RuntimeException e) {
            throw new RuntimeException("Registration failed: " + e.getMessage());
        }
    }
    
    // Login user
    public AuthResponse login(String email, String password) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
            );
            
            // Get user details
            Optional<User> userOptional = userService.findByEmail(email);
            if (userOptional.isEmpty()) {
                throw new RuntimeException("User not found");
            }
            
            User user = userOptional.get();
            
            return new AuthResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
            
        } catch (AuthenticationException e) {
            throw new RuntimeException("Invalid email or password");
        }
    }
    
    // Change password
    public void changePassword(String email, String currentPassword, String newPassword) {
        Optional<User> userOptional = userService.findByEmail(email);
        if (userOptional.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        
        User user = userOptional.get();
        userService.updatePassword(user.getId(), currentPassword, newPassword);
    }
    
    // Inner class for authentication response
    public static class AuthResponse {
        private final Long userId;
        private final String name;
        private final String email;
        private final UserRole role;
        
        public AuthResponse(Long userId, String name, String email, UserRole role) {
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.role = role;
        }
        
        // Getters
        public Long getUserId() { return userId; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public UserRole getRole() { return role; }
    }
}