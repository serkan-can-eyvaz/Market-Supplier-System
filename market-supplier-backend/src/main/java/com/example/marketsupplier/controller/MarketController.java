package com.example.marketsupplier.controller;

import com.example.marketsupplier.dto.*;
import com.example.marketsupplier.entity.Market;
import com.example.marketsupplier.entity.User;
import com.example.marketsupplier.entity.UserRole;
import com.example.marketsupplier.config.CustomUserDetailsService.CustomUserPrincipal;
import com.example.marketsupplier.service.AuthService;
import com.example.marketsupplier.service.MarketService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/markets")
@CrossOrigin(origins = "*")
public class MarketController {
    
    @Autowired
    private MarketService marketService;
    
    @Autowired
    private AuthService authService;
    
    // Create market
    @PostMapping
    public ResponseEntity<?> createMarket(@Valid @RequestBody MarketRequest marketRequest,
                                        Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            
            Market market = marketService.createMarket(
                userId,
                marketRequest.getName(),
                marketRequest.getAddress(),
                marketRequest.getPhone()
            );
            
            MarketResponse marketResponse = new MarketResponse(
                market.getId(),
                market.getName(),
                market.getAddress(),
                market.getPhone(),
                market.getUser().getId(),
                market.getUser().getName(),
                market.getUser().getEmail(),
                market.getCreatedAt()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(marketResponse);
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Failed to create market: " + e.getMessage()));
        }
    }
    
    // Get all markets
    @GetMapping
    public ResponseEntity<?> getAllMarkets(Authentication authentication) {
        try {
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied. Admin role required."));
            }
            
            List<Market> markets = marketService.getAllMarkets();
            List<MarketResponse> marketResponses = markets.stream()
                .map(market -> new MarketResponse(
                    market.getId(),
                    market.getName(),
                    market.getAddress(),
                    market.getPhone(),
                    market.getUser().getId(),
                    market.getUser().getName(),
                    market.getUser().getEmail(),
                    market.getCreatedAt()
                ))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(marketResponses);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve markets: " + e.getMessage()));
        }
    }
    
    // Get market by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getMarketById(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            
            // Check if user is admin or market owner (supplier who created it)
            if (!isAdmin(authentication) && !marketService.isMarketOwner(id, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied."));
            }
            
            var marketOptional = marketService.findById(id);
            if (marketOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Market market = marketOptional.get();
            MarketResponse marketResponse = new MarketResponse(
                market.getId(),
                market.getName(),
                market.getAddress(),
                market.getPhone(),
                market.getUser().getId(),
                market.getUser().getName(),
                market.getUser().getEmail(),
                market.getCreatedAt()
            );
            
            return ResponseEntity.ok(marketResponse);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve market: " + e.getMessage()));
        }
    }
    
    // Removed: my-market (single)

    // Get current user's markets (list) with pagination  
    @GetMapping("/my-markets")
    public ResponseEntity<?> getMyMarkets(Authentication authentication,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size,
                                        @RequestParam(defaultValue = "createdAt") String sortBy,
                                        @RequestParam(defaultValue = "desc") String sortDir) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }

            // Create pagination
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            
            Page<Market> marketPage = marketService.findAllByUserIdPaginated(userId, pageable);
            
            List<MarketResponse> marketResponses = marketPage.getContent().stream()
                .map(market -> new MarketResponse(
                    market.getId(),
                    market.getName(),
                    market.getAddress(),
                    market.getPhone(),
                    market.getUser().getId(),
                    market.getUser().getName(),
                    market.getUser().getEmail(),
                    market.getCreatedAt()
                ))
                .collect(Collectors.toList());
            
            PaginatedResponse<MarketResponse> paginatedResponse = new PaginatedResponse<>(
                marketResponses,
                marketPage.getNumber(),
                marketPage.getSize(),
                marketPage.getTotalElements(),
                marketPage.getTotalPages(),
                marketPage.isFirst(),
                marketPage.isLast()
            );
            
            return ResponseEntity.ok(paginatedResponse);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve markets: " + e.getMessage()));
        }
    }
    
    // Search markets by name
    @GetMapping("/search")
    public ResponseEntity<?> searchMarketsByName(@RequestParam String name, Authentication authentication) {
        try {
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied. Admin role required."));
            }
            
            List<Market> markets = marketService.searchMarketsByName(name);
            List<MarketResponse> marketResponses = markets.stream()
                .map(market -> new MarketResponse(
                    market.getId(),
                    market.getName(),
                    market.getAddress(),
                    market.getPhone(),
                    market.getUser().getId(),
                    market.getUser().getName(),
                    market.getUser().getEmail(),
                    market.getCreatedAt()
                ))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(marketResponses);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to search markets: " + e.getMessage()));
        }
    }
    
    // Update market
    @PutMapping("/{id}")
    public ResponseEntity<?> updateMarket(@PathVariable Long id,
                                        @Valid @RequestBody MarketRequest marketRequest,
                                        Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            
            // Check if user is admin or market owner
            if (!isAdmin(authentication) && !marketService.isMarketOwner(id, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied."));
            }
            
            Market updatedMarket = marketService.updateMarket(
                id,
                marketRequest.getName(),
                marketRequest.getAddress(),
                marketRequest.getPhone()
            );
            
            MarketResponse marketResponse = new MarketResponse(
                updatedMarket.getId(),
                updatedMarket.getName(),
                updatedMarket.getAddress(),
                updatedMarket.getPhone(),
                updatedMarket.getUser().getId(),
                updatedMarket.getUser().getName(),
                updatedMarket.getUser().getEmail(),
                updatedMarket.getCreatedAt()
            );
            
            return ResponseEntity.ok(marketResponse);
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Failed to update market: " + e.getMessage()));
        }
    }
    
    // Delete market
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMarket(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            
            // Admin veya market sahibi silebilir
            if (!isAdmin(authentication) && !marketService.isMarketOwner(id, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied. Only admin or market owner can delete."));
            }
            
            marketService.deleteMarket(id);
            return ResponseEntity.ok(new MessageResponse("Market deleted successfully"));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Failed to delete market: " + e.getMessage()));
        }
    }
    
    // Get market statistics
    @GetMapping("/stats")
    public ResponseEntity<?> getMarketStats(Authentication authentication) {
        try {
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied. Admin role required."));
            }
            
            MarketService.MarketStats stats = marketService.getMarketStats();
            return ResponseEntity.ok(stats);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve market statistics: " + e.getMessage()));
        }
    }
    
    // Helper methods
    private Long getUserIdFromAuthentication(Authentication authentication) {
        try {
            System.out.println("[MarketController] Authentication principal type: " + authentication.getPrincipal().getClass().getName());
            
            if (authentication.getPrincipal() instanceof CustomUserPrincipal) {
                CustomUserPrincipal userDetails = (CustomUserPrincipal) authentication.getPrincipal();
                System.out.println("[MarketController] User ID from CustomUserPrincipal: " + userDetails.getUser().getId());
                return userDetails.getUser().getId();
            } else if (authentication.getPrincipal() instanceof User) {
                User user = (User) authentication.getPrincipal();
                System.out.println("[MarketController] User ID from User: " + user.getId());
                return user.getId();
            } else {
                System.out.println("[MarketController] Unknown principal type: " + authentication.getPrincipal().getClass().getName());
                return null;
            }
        } catch (Exception e) {
            System.out.println("[MarketController] Error getting user ID: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    private boolean isAdmin(Authentication authentication) {
        try {
            if (authentication.getPrincipal() instanceof CustomUserPrincipal) {
                CustomUserPrincipal userDetails = (CustomUserPrincipal) authentication.getPrincipal();
                return userDetails.getUser().getRole() == UserRole.ADMIN;
            } else if (authentication.getPrincipal() instanceof User) {
                User user = (User) authentication.getPrincipal();
                return user.getRole() == UserRole.ADMIN;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    // Inner classes for responses
    public static class ErrorResponse {
        private String message;
        
        public ErrorResponse(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }
    
    public static class MessageResponse {
        private String message;
        
        public MessageResponse(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }
}
