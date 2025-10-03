package com.example.marketsupplier.controller;

import com.example.marketsupplier.service.JwtTokenService;
import com.example.marketsupplier.service.RateLimitService;
import com.example.marketsupplier.util.InputSanitizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import com.example.marketsupplier.service.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private InputSanitizer inputSanitizer;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        try {
            System.out.println("[AuthController] Login attempt for email: " + request.getEmail());
            
            // Input sanitization
            String email = inputSanitizer.sanitizeText(request.getEmail());
            String password = inputSanitizer.sanitizeText(request.getPassword());

            System.out.println("[AuthController] Sanitized email: " + email);

            // Rate limiting
            if (!rateLimitService.isAllowedPerMinute(email)) {
                System.out.println("[AuthController] Rate limit exceeded for: " + email);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(createErrorResponse("Rate limit exceeded", 60));
            }

            System.out.println("[AuthController] Attempting authentication...");
            
            // Authentication
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
            );
            
            System.out.println("[AuthController] Authentication successful for: " + email);

            // Get user to determine correct role
            Optional<com.example.marketsupplier.entity.User> userOpt = userService.findByEmail(email);
            String userRole = "USER"; // Default role
            Long userId = 1L; // Default user ID
            String userName = "User"; // Default name
            
            if (userOpt.isPresent()) {
                com.example.marketsupplier.entity.User user = userOpt.get();
                userRole = user.getRole().name(); // Get actual role from database
                userId = user.getId();
                userName = user.getName();
                System.out.println("[AuthController] User role from DB: " + userRole);
            }

            // Generate tokens with correct role
            String accessToken = jwtTokenService.generateToken(email, userRole);
            String refreshToken = jwtTokenService.generateRefreshToken(email, userRole);
            
            System.out.println("[AuthController] Generated access token: " + accessToken);
            System.out.println("[AuthController] Generated refresh token: " + refreshToken);

            Map<String, Object> response = new HashMap<>();
            response.put("token", accessToken);
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken);
            response.put("tokenType", "Bearer");
            response.put("expiresIn", 3600); // 1 hour
            response.put("email", email);
            response.put("userId", userId);
            response.put("name", userName);
            response.put("role", userRole);

            return ResponseEntity.ok(response);

        } catch (InputSanitizer.SecurityException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse("Invalid input format", 0));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(createErrorResponse("Authentication failed", 0));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        try {
            // Input sanitization
            String name = inputSanitizer.sanitizeText(request.getName());
            String email = inputSanitizer.sanitizeText(request.getEmail());
            String password = inputSanitizer.sanitizeText(request.getPassword());

            // Rate limiting
            if (!rateLimitService.isAllowedPerMinute(email)) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(createErrorResponse("Rate limit exceeded", 60));
            }

            // For now, just create a simple user registration
            // In a real app, you'd save to database and hash password
            String accessToken = jwtTokenService.generateToken(email, "USER");
            String refreshToken = jwtTokenService.generateRefreshToken(email);

            Map<String, Object> response = new HashMap<>();
            response.put("token", accessToken);
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken);
            response.put("tokenType", "Bearer");
            response.put("expiresIn", 3600); // 1 hour
            response.put("email", email);
            response.put("userId", 2); // New user ID
            response.put("name", name);
            response.put("role", "USER");

            return ResponseEntity.ok(response);

        } catch (InputSanitizer.SecurityException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse("Invalid input format", 0));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Registration failed", 0));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@RequestBody RefreshRequest request) {
        try {
            // Input sanitization
            String refreshToken = inputSanitizer.sanitizeText(request.getRefreshToken());

            // Validate refresh token
            if (!jwtTokenService.validateToken(refreshToken) || 
                !"refresh".equals(jwtTokenService.getTokenType(refreshToken))) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Invalid refresh token", 0));
            }

            String phone = jwtTokenService.getUsernameFromToken(refreshToken);
            String originalRole = jwtTokenService.getRoleFromToken(refreshToken);

            // Rate limiting
            if (!rateLimitService.isAllowedPerMinute(phone)) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(createErrorResponse("Rate limit exceeded", 60));
            }

            // Generate new access token with original role
            String newAccessToken = jwtTokenService.generateToken(phone, originalRole != null ? originalRole : "USER");

            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", newAccessToken);
            response.put("tokenType", "Bearer");
            response.put("expiresIn", 3600); // 1 hour
            response.put("phone", phone);

            return ResponseEntity.ok(response);

        } catch (InputSanitizer.SecurityException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse("Invalid input format", 0));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(createErrorResponse("Token refresh failed", 0));
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(@RequestBody ValidateRequest request) {
        try {
            // Input sanitization
            String token = inputSanitizer.sanitizeText(request.getToken());

            // Validate token
            boolean isValid = jwtTokenService.validateToken(token);
            
            Map<String, Object> response = new HashMap<>();
            response.put("valid", isValid);
            
            if (isValid) {
                response.put("phone", jwtTokenService.getUsernameFromToken(token));
                response.put("role", jwtTokenService.getRoleFromToken(token));
                response.put("expired", jwtTokenService.isTokenExpired(token));
            }

            return ResponseEntity.ok(response);

        } catch (InputSanitizer.SecurityException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse("Invalid input format", 0));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(createErrorResponse("Token validation failed", 0));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            // Extract token from header
            String token = authHeader.replace("Bearer ", "");
            
            // Validate token
            if (!jwtTokenService.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Invalid token", 0));
            }

            String phone = jwtTokenService.getUsernameFromToken(token);
            
            // Reset rate limit for user
            rateLimitService.resetRateLimit(phone);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Logged out successfully");
            response.put("phone", phone);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Logout failed", 0));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            // Extract token from header
            String token = authHeader.replace("Bearer ", "");
            
            // Validate token
            if (!jwtTokenService.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Invalid token", 0));
            }

            String email = jwtTokenService.getUsernameFromToken(token);
            String role = jwtTokenService.getRoleFromToken(token);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", 1); // Default admin user
            response.put("name", "Admin User");
            response.put("email", email);
            response.put("role", role);
            response.put("createdAt", new java.util.Date().toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to get user info", 0));
        }
    }

    private Map<String, Object> createErrorResponse(String message, int retryAfter) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", message);
        if (retryAfter > 0) {
            response.put("retryAfter", retryAfter);
        }
        return response;
    }

    // Request DTOs
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

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RefreshRequest {
        private String refreshToken;

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }

    public static class ValidateRequest {
        private String token;

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }
}