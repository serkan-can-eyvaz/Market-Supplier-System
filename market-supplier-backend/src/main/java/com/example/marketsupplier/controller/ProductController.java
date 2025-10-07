package com.example.marketsupplier.controller;

import com.example.marketsupplier.entity.Product;
import com.example.marketsupplier.entity.User;
import com.example.marketsupplier.service.ProductService;
import com.example.marketsupplier.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:3000")
public class ProductController {

    @Autowired
    private ProductService productService;

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
            User user = (User) authentication.getPrincipal();
            
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
            
            return ResponseEntity.ok(paginatedResponse);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/supplier/all")
    public ResponseEntity<?> getAllSupplierProducts(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            List<Product> products = productService.getAllSupplierProducts(user);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createProduct(
            Authentication authentication,
            @Valid @RequestBody ProductCreateRequest request) {
        try {
            User user = (User) authentication.getPrincipal();
            
            Product product = productService.createProduct(user, request.getName(), request.getDescription(), 
                                                          request.getUnit(), request.getPrice(), request.getStockQuantity());
            
            // Convert to response DTO
            ProductResponse response = convertToProductResponse(product);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request) {
        try {
            User user = (User) authentication.getPrincipal();
            
            Product product = productService.updateProduct(user, id, request);
            
            // Convert to response DTO
            ProductResponse response = convertToProductResponse(product);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/stock")
    public ResponseEntity<?> updateStock(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam Integer stockQuantity) {
        try {
            User user = (User) authentication.getPrincipal();
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
            User user = (User) authentication.getPrincipal();
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
            User user = (User) authentication.getPrincipal();
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
            User user = (User) authentication.getPrincipal();
            
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

    // Market kullanıcıları için tüm aktif ürünleri getir
    @GetMapping("/market/available")
    public ResponseEntity<?> getAvailableProductsForMarket(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            
            // Sadece MARKET rolü bu endpoint'i kullanabilir
            if (!user.getRole().name().equals("MARKET")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied. Market role required.");
            }
            
            List<Product> products = productService.getAllActiveProducts();
            List<ProductResponse> responses = products.stream()
                    .map(this::convertToProductResponse)
                    .toList();
            
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving products: " + e.getMessage());
        }
    }

    // Tedarikçi ürünlerini ProductResponse formatında getir
    @GetMapping("/supplier/formatted")
    public ResponseEntity<?> getSupplierProductsFormatted(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            
            List<Product> products = productService.getSupplierProducts(user);
            List<ProductResponse> responses = products.stream()
                    .map(this::convertToProductResponse)
                    .toList();
            
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving products: " + e.getMessage());
        }
    }

    // Helper method to convert Product to ProductResponse
    private ProductResponse convertToProductResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getUnit(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getIsActive(),
                product.getSupplier() != null ? product.getSupplier().getCompanyName() : null,
                product.getSupplier() != null ? product.getSupplier().getId() : null,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}