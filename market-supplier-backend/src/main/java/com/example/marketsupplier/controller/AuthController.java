package com.example.marketsupplier.controller;

import com.example.marketsupplier.service.UserService;
import com.example.marketsupplier.service.JwtService;
import com.example.marketsupplier.service.MarketService;
import com.example.marketsupplier.entity.User;
import com.example.marketsupplier.entity.Market;
import com.example.marketsupplier.config.CustomUserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private MarketService marketService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

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

            // Generate JWT token for new user
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
            response.put("message", "Kayıt başarılı");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("[AuthController] Register failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Kayıt sırasında hata: " + e.getMessage()));
        }
    }

    @PostMapping("/login-phone")
    public ResponseEntity<Map<String, Object>> loginWithPhone(@RequestBody PhoneLoginRequest request) {
        try {
            System.out.println("[AuthController] Phone login attempt for: " + request.getPhone());
            
            // Telefon numarasını normalize et
            String normalizedPhone = normalizePhone(request.getPhone());
            System.out.println("[AuthController] Normalized phone: " + normalizedPhone);
            
            // Telefon numarasına sahip market'i bul
            Optional<Market> marketOpt = marketService.findByPhoneNormalized(normalizedPhone);
            if (!marketOpt.isPresent()) {
                System.out.println("[AuthController] Market not found for phone: " + normalizedPhone);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Bu telefon numarasına kayıtlı market bulunamadı"));
            }
            
            Market market = marketOpt.get();
            User user = market.getUser();
            
            System.out.println("[AuthController] Market found: " + market.getName() + " - User: " + user.getEmail());
            
            // Şifre kontrolü
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                System.out.println("[AuthController] Password mismatch for user: " + user.getEmail());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Geçersiz şifre"));
            }
            
            // JWT token oluştur
            String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
            
            // Response oluştur
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
            response.put("market", Map.of(
                    "id", market.getId(),
                    "name", market.getName(),
                    "phone", market.getPhone()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("[AuthController] Phone login failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Giriş başarısız: " + e.getMessage()));
        }
    }
    
    private String normalizePhone(String phone) {
        if (phone == null) return "";
        // Sadece rakamları al
        String digits = phone.replaceAll("[^0-9]", "");
        
        // Türkiye telefon numarası formatını kontrol et
        if (digits.startsWith("905")) {
            // +905348865278 -> 5348865278
            return digits.substring(3);
        } else if (digits.startsWith("05")) {
            // 05348865278 -> 5348865278
            return digits.substring(1);
        } else if (digits.startsWith("534") && digits.length() == 10) {
            // 5348865278 -> 5348865278 (zaten doğru format)
            return digits;
        }
        
        return digits;
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
    
    public static class PhoneLoginRequest {
        private String phone;
        private String password;

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}