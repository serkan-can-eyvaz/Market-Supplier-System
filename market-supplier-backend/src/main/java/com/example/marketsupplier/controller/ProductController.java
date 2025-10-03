package com.example.marketsupplier.controller;

import com.example.marketsupplier.entity.Product;
import com.example.marketsupplier.entity.User;
import com.example.marketsupplier.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.example.marketsupplier.dto.PaginatedResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.example.marketsupplier.service.UserService;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:3000")
public class ProductController {

    @Autowired
    private ProductService productService;
    
    @Autowired
    private UserService userService;

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("ProductController is working!");
    }

    @GetMapping
    public ResponseEntity<?> getSupplierProducts(Authentication authentication,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "10") int size,
                                               @RequestParam(defaultValue = "createdAt") String sortBy,
                                               @RequestParam(defaultValue = "desc") String sortDir) {
        try {
            System.out.println("DEBUG: ProductController.getSupplierProducts called");
            Object principal = authentication.getPrincipal();
            System.out.println("DEBUG: Principal type: " + principal.getClass().getName());
            
            User user;
            if (principal instanceof User) {
                user = (User) principal;
            } else if (principal instanceof com.example.marketsupplier.config.CustomUserDetailsService.CustomUserPrincipal) {
                // CustomUserPrincipal'dan User'a dönüştür
                user = ((com.example.marketsupplier.config.CustomUserDetailsService.CustomUserPrincipal) principal).getUser();
            } else {
                // JWT authentication'dan gelen String username'i User'a dönüştür
                String email = (String) principal;
                Optional<User> userOpt = userService.findByEmail(email);
                if (userOpt.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
                }
                user = userOpt.get();
            }
            
            System.out.println("DEBUG: User: " + user.getEmail() + ", Role: " + user.getRole());
            
            // Create pagination
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            
            Page<Product> productPage = productService.getAllSupplierProductsPaginated(user, pageable);
            
            PaginatedResponse<Product> paginatedResponse = new PaginatedResponse<>(
                productPage.getContent(),
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.isFirst(),
                productPage.isLast()
            );
            
            System.out.println("DEBUG: Products count: " + productPage.getTotalElements());
            return ResponseEntity.ok(paginatedResponse);
        } catch (Exception e) {
            System.out.println("DEBUG: ProductController.getSupplierProducts error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/supplier/all")
    public ResponseEntity<?> getAllSupplierProducts(Authentication authentication) {
        try {
            Object principal = authentication.getPrincipal();
            User user;
            if (principal instanceof User) {
                user = (User) principal;
            } else if (principal instanceof com.example.marketsupplier.config.CustomUserDetailsService.CustomUserPrincipal) {
                user = ((com.example.marketsupplier.config.CustomUserDetailsService.CustomUserPrincipal) principal).getUser();
            } else {
                // JWT authentication'dan gelen String username'i User'a dönüştür
                String email = (String) principal;
                Optional<User> userOpt = userService.findByEmail(email);
                if (userOpt.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
                }
                user = userOpt.get();
            }
            List<Product> products = productService.getAllSupplierProducts(user);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createProduct(
            Authentication authentication,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam String unit,
            @RequestParam String price) {
        try {
            System.out.println("DEBUG: ProductController.createProduct called with price: " + price);
            Object principal = authentication.getPrincipal();
            User user;
            if (principal instanceof User) {
                user = (User) principal;
            } else if (principal instanceof com.example.marketsupplier.config.CustomUserDetailsService.CustomUserPrincipal) {
                user = ((com.example.marketsupplier.config.CustomUserDetailsService.CustomUserPrincipal) principal).getUser();
            } else {
                // JWT authentication'dan gelen String username'i User'a dönüştür
                String email = (String) principal;
                Optional<User> userOpt = userService.findByEmail(email);
                if (userOpt.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
                }
                user = userOpt.get();
            }
            BigDecimal priceDecimal = new BigDecimal(price);
            Product product = productService.createProduct(user, name, description, unit, priceDecimal);
            return ResponseEntity.ok(product);
        } catch (Exception e) {
            System.out.println("DEBUG: ProductController.createProduct error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam String unit,
            @RequestParam BigDecimal price) {
        try {
            Object principal = authentication.getPrincipal();
            User user;
            if (principal instanceof User) {
                user = (User) principal;
            } else if (principal instanceof com.example.marketsupplier.config.CustomUserDetailsService.CustomUserPrincipal) {
                user = ((com.example.marketsupplier.config.CustomUserDetailsService.CustomUserPrincipal) principal).getUser();
            } else {
                // JWT authentication'dan gelen String username'i User'a dönüştür
                String email = (String) principal;
                Optional<User> userOpt = userService.findByEmail(email);
                if (userOpt.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
                }
                user = userOpt.get();
            }
            Product product = productService.updateProduct(user, id, name, description, unit, price);
            return ResponseEntity.ok(product);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/stock")
    public ResponseEntity<?> updateStock(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam Integer stockQuantity) {
        try {
            Object principal = authentication.getPrincipal();
            User user;
            if (principal instanceof User) {
                user = (User) principal;
            } else if (principal instanceof com.example.marketsupplier.config.CustomUserDetailsService.CustomUserPrincipal) {
                user = ((com.example.marketsupplier.config.CustomUserDetailsService.CustomUserPrincipal) principal).getUser();
            } else {
                // JWT authentication'dan gelen String username'i User'a dönüştür
                String email = (String) principal;
                Optional<User> userOpt = userService.findByEmail(email);
                if (userOpt.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
                }
                user = userOpt.get();
            }
            Product product = productService.updateStock(user, id, stockQuantity);
            return ResponseEntity.ok(product);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(Authentication authentication, @PathVariable Long id) {
        try {
            User user = (User) authentication.getPrincipal();
            productService.deleteProduct(user, id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<?> toggleProductStatus(Authentication authentication, @PathVariable Long id) {
        try {
            Object principal = authentication.getPrincipal();
            User user;
            if (principal instanceof User) {
                user = (User) principal;
            } else if (principal instanceof com.example.marketsupplier.config.CustomUserDetailsService.CustomUserPrincipal) {
                user = ((com.example.marketsupplier.config.CustomUserDetailsService.CustomUserPrincipal) principal).getUser();
            } else {
                // JWT authentication'dan gelen String username'i User'a dönüştür
                String email = (String) principal;
                Optional<User> userOpt = userService.findByEmail(email);
                if (userOpt.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
                }
                user = userOpt.get();
            }
            Product product = productService.toggleProductStatus(user, id);
            return ResponseEntity.ok(product);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(
            Authentication authentication,
            @RequestParam String q) {
        try {
            User user = (User) authentication.getPrincipal();
            List<Product> products = productService.searchProducts(user, q);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProduct(Authentication authentication, @PathVariable Long id) {
        try {
            Object principal = authentication.getPrincipal();
            User user;
            if (principal instanceof User) {
                user = (User) principal;
            } else if (principal instanceof com.example.marketsupplier.config.CustomUserDetailsService.CustomUserPrincipal) {
                user = ((com.example.marketsupplier.config.CustomUserDetailsService.CustomUserPrincipal) principal).getUser();
            } else {
                // JWT authentication'dan gelen String username'i User'a dönüştür
                String email = (String) principal;
                Optional<User> userOpt = userService.findByEmail(email);
                if (userOpt.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
                }
                user = userOpt.get();
            }
            return productService.getProductById(user, id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // AI Agent için tüm aktif ürünleri getir (authentication olmadan)
    @GetMapping("/ai/available")
    public ResponseEntity<List<Product>> getAvailableProductsForAI() {
        try {
            List<Product> products = productService.getAllActiveProducts();
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Admin için tüm ürünleri getir (kalem olarak)
    @GetMapping("/all")
    public ResponseEntity<?> getAllProducts(Authentication authentication) {
        try {
            Object principal = authentication.getPrincipal();
            User user;
            
            if (principal instanceof User) {
                user = (User) principal;
            } else if (principal instanceof com.example.marketsupplier.config.CustomUserDetailsService.CustomUserPrincipal) {
                user = ((com.example.marketsupplier.config.CustomUserDetailsService.CustomUserPrincipal) principal).getUser();
            } else if (principal instanceof String) {
                // JWT authentication
                String email = (String) principal;
                Optional<User> userOpt = userService.findByEmail(email);
                if (userOpt.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
                }
                user = userOpt.get();
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid authentication");
            }
            
            // Sadece ADMIN rolü tüm ürünleri görebilir
            if (!user.getRole().name().equals("ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied. Admin role required.");
            }
            
            List<Product> products = productService.getAllProducts();
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving products: " + e.getMessage());
        }
    }
}
