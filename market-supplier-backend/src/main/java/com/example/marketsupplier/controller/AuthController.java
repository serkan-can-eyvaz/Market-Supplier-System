package com.example.marketsupplier.controller;

import com.example.marketsupplier.service.UserService;
import com.example.marketsupplier.service.JwtService;
import com.example.marketsupplier.entity.User;
import com.example.marketsupplier.config.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        try {
            System.out.println("[AuthController] Login attempt for: " + request.getEmail());
            System.out.println("[AuthController] Password length: " + (request.getPassword() != null ? request.getPassword().length() : "null"));
            
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            System.out.println("[AuthController] Authentication successful");
            System.out.println("[AuthController] Authentication details: " + authentication.getDetails());

            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Get user details
            CustomUserDetailsService.CustomUserPrincipal userPrincipal = (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
            User user = userPrincipal.getUser();
            System.out.println("[AuthController] User found: " + user.getEmail() + " - Role: " + user.getRole());

                // Generate JWT token
                String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
                
                // Create response
                Map<String, Object> response = new HashMap<>();
                response.put("user", Map.of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "name", user.getName(),
                        "role", user.getRole().name(),
                        "createdAt", user.getCreatedAt().toString()
                ));
                response.put("token", token);
                response.put("message", "Giriş başarılı");

                return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("[AuthController] Authentication failed: " + e.getMessage());
            System.out.println("[AuthController] Exception type: " + e.getClass().getSimpleName());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Geçersiz email veya şifre"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        try {
            System.out.println("[AuthController] Register attempt for: " + request.getEmail());
            System.out.println("[AuthController] Register password length: " + (request.getPassword() != null ? request.getPassword().length() : "null"));
            
            // Check if user already exists
            if (userService.findByEmail(request.getEmail()).isPresent()) {
                System.out.println("[AuthController] User already exists: " + request.getEmail());
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Bu email adresi zaten kullanımda"));
            }

            // Create new user
            User user = userService.createUser(
                    request.getName(),
                    request.getEmail(),
                    request.getPassword(),
                    request.getRole()
            );

            System.out.println("[AuthController] User created successfully: " + user.getEmail());
            System.out.println("[AuthController] User password hash: " + user.getPassword());

            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("user", Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "name", user.getName(),
                    "role", user.getRole().name(),
                    "createdAt", user.getCreatedAt().toString()
            ));
            response.put("message", "Kayıt başarılı");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("[AuthController] Register failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Kayıt sırasında hata: " + e.getMessage()));
        }
    }



    // DTO sınıfları
    public static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RegisterRequest {
        private String name;
        private String email;
        private String password;
        private com.example.marketsupplier.entity.UserRole role;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public com.example.marketsupplier.entity.UserRole getRole() { return role; }
        public void setRole(com.example.marketsupplier.entity.UserRole role) { this.role = role; }
    }
}