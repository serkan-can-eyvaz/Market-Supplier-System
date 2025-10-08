package com.example.marketsupplier.config;

import com.example.marketsupplier.entity.User;
import com.example.marketsupplier.entity.UserRole;
import com.example.marketsupplier.service.UserService;
import com.example.marketsupplier.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements CommandLineRunner {

    @Value("${app.admin.bootstrap.enabled:true}")
    private boolean bootstrapEnabled;

    @Value("${app.admin.bootstrap.email:admin@system.local}")
    private String adminEmail;

    @Value("${app.admin.bootstrap.name:System Admin}")
    private String adminName;

    @Value("${app.admin.bootstrap.password:admin123}")
    private String adminPassword;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Override
    public void run(String... args) {
        if (!bootstrapEnabled) {
            return;
        }

        long adminCount = userRepository.countByRole(UserRole.ADMIN);
        System.out.println("[AdminBootstrap] Current admin count: " + adminCount);
        
        if (adminCount == 0) {
            // Create default admin if none exists
            try {
                userService.createUser(adminName, adminEmail, adminPassword, UserRole.ADMIN);
                System.out.println("[AdminBootstrap] Default admin created: " + adminEmail);
            } catch (RuntimeException e) {
                System.err.println("[AdminBootstrap] Failed to create default admin: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // Admin already exists, no need to update password
            System.out.println("[AdminBootstrap] Admin already exists: " + adminEmail);
        }
        
        // Create test users for JWT testing
        createTestUsers();
    }
    
    private void createTestUsers() {
        System.out.println("[AdminBootstrap] Creating test users for JWT...");
        
        // Test Admin
        createTestUserIfNotExists("Test Admin", "admin@test.com", "admin123", UserRole.ADMIN);
        
        // Test Supplier
        createTestUserIfNotExists("Test Supplier", "supplier@test.com", "supplier123", UserRole.SUPPLIER);
        
        // Test Market Owner
        createTestUserIfNotExists("Test Market Owner", "market@test.com", "market123", UserRole.MARKET);
        
        System.out.println("[AdminBootstrap] Test users creation completed.");
    }
    
    private void createTestUserIfNotExists(String name, String email, String password, UserRole role) {
        try {
            if (!userRepository.findByEmail(email).isPresent()) {
                userService.createUser(name, email, password, role);
                System.out.println("[AdminBootstrap] Test user created: " + email + " (" + role + ")");
            } else {
                System.out.println("[AdminBootstrap] Test user already exists: " + email);
            }
        } catch (Exception e) {
            System.err.println("[AdminBootstrap] Failed to create test user " + email + ": " + e.getMessage());
        }
    }
}


