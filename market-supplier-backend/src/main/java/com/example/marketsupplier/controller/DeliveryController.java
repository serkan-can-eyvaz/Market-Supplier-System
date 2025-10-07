package com.example.marketsupplier.controller;

import com.example.marketsupplier.dto.*;
import com.example.marketsupplier.entity.*;
import com.example.marketsupplier.service.AuthService;
import com.example.marketsupplier.service.DeliveryService;
import com.example.marketsupplier.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/deliveries")
@CrossOrigin(origins = "*")
public class DeliveryController {
    
    @Autowired
    private DeliveryService deliveryService;

    
    @Autowired
    private SupplierService supplierService;
    
    @Autowired
    private AuthService authService;
    

    // Basit canlı ETA push: stopsAhead azaldığında marketi haberdar et
    @PostMapping("/{deliveryId}/progress")
    public ResponseEntity<?> updateProgress(@PathVariable Long deliveryId,
                                            @RequestBody String routeInfoJson) {
        try {
            var dOpt = deliveryService.findById(deliveryId);
            if (dOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Delivery not found"));
            var d = dOpt.get();
            String oldInfo = d.getRouteInfo();
            // routeInfo güncelle
            deliveryService.planDeliveryRoute(deliveryId, routeInfoJson);
            // stopsAhead karşılaştır
            Integer oldStops = extractStopsAhead(oldInfo);
            Integer newStops = extractStopsAhead(routeInfoJson);
            // WhatsApp notification removed - no longer needed
            return ResponseEntity.ok(new MessageResponse("Progress updated"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Failed to update progress"));
        }
    }

    private Integer extractStopsAhead(String routeInfo) {
        try {
            if (routeInfo == null) return null;
            if (routeInfo.contains("stopsAhead")) {
                String num = routeInfo.replaceAll(".*stopsAhead[^0-9]*([0-9]+).*", "$1");
                if (num != null && num.matches("[0-9]+")) return Integer.parseInt(num);
            }
        } catch (Exception ignored) {}
        return null;
    }
    
    // Create delivery assignment
    @PostMapping
    public ResponseEntity<?> createDelivery(@Valid @RequestBody DeliveryRequest deliveryRequest,
                                          Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            
            // Check if user is admin or supplier
            if (!isAdmin(authentication) && !isSupplier(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied. Admin or Supplier role required."));
            }
            
            // If supplier, check if they own the supplier
            if (isSupplier(authentication) && !supplierService.isSupplierOwner(deliveryRequest.getSupplierId(), userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied."));
            }
            
            Delivery delivery = deliveryService.createDelivery(
                deliveryRequest.getOrderId(),
                deliveryRequest.getSupplierId()
            );
            
            DeliveryResponse deliveryResponse = new DeliveryResponse(
                delivery.getId(),
                delivery.getOrder().getId(),
                delivery.getOrder().getMarket().getName(),
                delivery.getOrder().getMarket().getAddress(),
                delivery.getSupplier().getId(),
                delivery.getSupplier().getCompanyName(),
                delivery.getDeliveryStatus(),
                delivery.getCreatedAt()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(deliveryResponse);
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Failed to create delivery: " + e.getMessage()));
        }
    }
    

    
    // Complete delivery
    @PostMapping("/{deliveryId}/complete")
    public ResponseEntity<?> completeDelivery(@PathVariable Long deliveryId,
                                            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            
            // Check if user owns the delivery (convert userId to supplierId)
            boolean hasAccess = isAdmin(authentication);
            if (!hasAccess) {
                var supplierOptional = supplierService.findByUserId(userId);
                if (supplierOptional.isPresent()) {
                    hasAccess = deliveryService.isDeliveryOwner(deliveryId, supplierOptional.get().getId());
                }
            }
            
            if (!hasAccess) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied."));
            }
            
            Delivery delivery = deliveryService.completeDelivery(deliveryId);
            
            DeliveryResponse deliveryResponse = new DeliveryResponse(
                delivery.getId(),
                delivery.getOrder().getId(),
                delivery.getOrder().getMarket().getName(),
                delivery.getOrder().getMarket().getAddress(),
                delivery.getSupplier().getId(),
                delivery.getSupplier().getCompanyName(),
                delivery.getDeliveryStatus(),
                delivery.getCreatedAt()
            );
            deliveryResponse.setDeliveryTime(delivery.getDeliveryTime());
            deliveryResponse.setRouteInfo(delivery.getRouteInfo());
            deliveryResponse.setUpdatedAt(delivery.getUpdatedAt());
            
            return ResponseEntity.ok(deliveryResponse);
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Failed to complete delivery: " + e.getMessage()));
        }
    }
    
    // Get deliveries by supplier
    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<?> getDeliveriesBySupplier(@PathVariable Long supplierId,
                                                    Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            
            // Check if user is admin or supplier owner
            if (!isAdmin(authentication) && !supplierService.isSupplierOwner(supplierId, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied."));
            }
            
            List<Delivery> deliveries = deliveryService.getDeliveriesBySupplier(supplierId);
            List<DeliveryResponse> deliveryResponses = deliveries.stream()
                .map(delivery -> {
                    DeliveryResponse response = new DeliveryResponse(
                        delivery.getId(),
                        delivery.getOrder().getId(),
                        delivery.getOrder().getMarket().getName(),
                        delivery.getOrder().getMarket().getAddress(),
                        delivery.getSupplier().getId(),
                        delivery.getSupplier().getCompanyName(),
                        delivery.getDeliveryStatus(),
                        delivery.getCreatedAt()
                    );
                    response.setDeliveryTime(delivery.getDeliveryTime());
                    response.setRouteInfo(delivery.getRouteInfo());
                    response.setUpdatedAt(delivery.getUpdatedAt());
                    return response;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(deliveryResponses);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve deliveries: " + e.getMessage()));
        }
    }
    
    // Get my deliveries (for current supplier) with pagination
    @GetMapping("/my-deliveries")
    public ResponseEntity<?> getMyDeliveries(Authentication authentication,
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
            
            if (!isSupplier(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied. Supplier role required."));
            }
            
            // Get user's supplier
            var supplierOptional = supplierService.findByUserId(userId);
            if (supplierOptional.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Supplier not found for user"));
            }
            
            // Opportunistic backfill: create deliveries if some approved orders had none
            try { deliveryService.createDeliveriesForApprovedWithoutDelivery(supplierOptional.get().getId()); } catch (Exception ignored) {}
            
            // Create pagination
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            
            Page<Delivery> deliveryPage = deliveryService.getDeliveriesBySupplierPaginated(supplierOptional.get().getId(), pageable);
            
            List<DeliveryResponse> deliveryResponses = deliveryPage.getContent().stream()
                .map(delivery -> {
                    DeliveryResponse response = new DeliveryResponse(
                        delivery.getId(),
                        delivery.getOrder().getId(),
                        delivery.getOrder().getMarket().getName(),
                        delivery.getOrder().getMarket().getAddress(),
                        delivery.getOrder().getMarket().getLatitude(),
                        delivery.getOrder().getMarket().getLongitude(),
                        delivery.getSupplier().getId(),
                        delivery.getSupplier().getCompanyName(),
                        delivery.getDeliveryStatus(),
                        delivery.getCreatedAt()
                    );
                    response.setDeliveryTime(delivery.getDeliveryTime());
                    response.setRouteInfo(delivery.getRouteInfo());
                    response.setUpdatedAt(delivery.getUpdatedAt());
                    return response;
                })
                .collect(Collectors.toList());
            
            PaginatedResponse<DeliveryResponse> paginatedResponse = new PaginatedResponse<>(
                deliveryResponses,
                deliveryPage.getNumber(),
                deliveryPage.getSize(),
                deliveryPage.getTotalElements(),
                deliveryPage.getTotalPages(),
                deliveryPage.isFirst(),
                deliveryPage.isLast()
            );
            
            return ResponseEntity.ok(paginatedResponse);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve deliveries: " + e.getMessage()));
        }
    }
    
    // Get all deliveries (Admin only) with pagination
    @GetMapping("/all")
    public ResponseEntity<?> getAllDeliveries(Authentication authentication,
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
            
            // Only admin can access all deliveries
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied. Admin role required."));
            }
            
            // Create pagination
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            
            Page<Delivery> deliveryPage = deliveryService.getAllDeliveriesPaginated(pageable);
            
            List<DeliveryResponse> deliveryResponses = deliveryPage.getContent().stream()
                .map(delivery -> {
                    DeliveryResponse response = new DeliveryResponse(
                        delivery.getId(),
                        delivery.getOrder().getId(),
                        delivery.getOrder().getMarket().getName(),
                        delivery.getOrder().getMarket().getAddress(),
                        delivery.getOrder().getMarket().getLatitude(),
                        delivery.getOrder().getMarket().getLongitude(),
                        delivery.getSupplier().getId(),
                        delivery.getSupplier().getCompanyName(),
                        delivery.getDeliveryStatus(),
                        delivery.getCreatedAt()
                    );
                    response.setDeliveryTime(delivery.getDeliveryTime());
                    response.setRouteInfo(delivery.getRouteInfo());
                    response.setUpdatedAt(delivery.getUpdatedAt());
                    return response;
                })
                .collect(Collectors.toList());
            
            PaginatedResponse<DeliveryResponse> paginatedResponse = new PaginatedResponse<>(
                deliveryResponses,
                deliveryPage.getNumber(),
                deliveryPage.getSize(),
                deliveryPage.getTotalElements(),
                deliveryPage.getTotalPages(),
                deliveryPage.isFirst(),
                deliveryPage.isLast()
            );
            
            return ResponseEntity.ok(paginatedResponse);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve deliveries: " + e.getMessage()));
        }
    }
    
    // Get in-progress deliveries for supplier
    @GetMapping("/supplier/{supplierId}/in-progress")
    public ResponseEntity<?> getInProgressDeliveriesForSupplier(@PathVariable Long supplierId,
                                                               Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            
            // Check if user is admin or supplier owner
            if (!isAdmin(authentication) && !supplierService.isSupplierOwner(supplierId, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied."));
            }
            
            List<Delivery> deliveries = deliveryService.getInProgressDeliveriesForSupplier(supplierId);
            List<DeliveryResponse> deliveryResponses = deliveries.stream()
                .map(delivery -> {
                    DeliveryResponse response = new DeliveryResponse(
                        delivery.getId(),
                        delivery.getOrder().getId(),
                        delivery.getOrder().getMarket().getName(),
                        delivery.getOrder().getMarket().getAddress(),
                        delivery.getSupplier().getId(),
                        delivery.getSupplier().getCompanyName(),
                        delivery.getDeliveryStatus(),
                        delivery.getCreatedAt()
                    );
                    response.setRouteInfo(delivery.getRouteInfo());
                    return response;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(deliveryResponses);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve in-progress deliveries: " + e.getMessage()));
        }
    }
    
    // Get daily deliveries for supplier
    @GetMapping("/supplier/{supplierId}/daily")
    public ResponseEntity<?> getDailyDeliveriesForSupplier(@PathVariable Long supplierId,
                                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date,
                                                          Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            
            // Check if user is admin or supplier owner
            if (!isAdmin(authentication) && !supplierService.isSupplierOwner(supplierId, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied."));
            }
            
            List<Delivery> deliveries = deliveryService.getDailyDeliveriesForSupplier(supplierId, date);
            List<DeliveryResponse> deliveryResponses = deliveries.stream()
                .map(delivery -> {
                    DeliveryResponse response = new DeliveryResponse(
                        delivery.getId(),
                        delivery.getOrder().getId(),
                        delivery.getOrder().getMarket().getName(),
                        delivery.getOrder().getMarket().getAddress(),
                        delivery.getSupplier().getId(),
                        delivery.getSupplier().getCompanyName(),
                        delivery.getDeliveryStatus(),
                        delivery.getCreatedAt()
                    );
                    response.setDeliveryTime(delivery.getDeliveryTime());
                    response.setRouteInfo(delivery.getRouteInfo());
                    response.setUpdatedAt(delivery.getUpdatedAt());
                    return response;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(deliveryResponses);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve daily deliveries: " + e.getMessage()));
        }
    }
    
    // Get delivery by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getDeliveryById(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            
            var deliveryOptional = deliveryService.findById(id);
            if (deliveryOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Delivery delivery = deliveryOptional.get();
            
            // Check access permissions
            boolean hasAccess = isAdmin(authentication) || 
                              deliveryService.isDeliveryOwner(id, userId);
            
            if (!hasAccess) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied."));
            }
            
            DeliveryResponse deliveryResponse = new DeliveryResponse(
                delivery.getId(),
                delivery.getOrder().getId(),
                delivery.getOrder().getMarket().getName(),
                delivery.getOrder().getMarket().getAddress(),
                delivery.getSupplier().getId(),
                delivery.getSupplier().getCompanyName(),
                delivery.getDeliveryStatus(),
                delivery.getCreatedAt()
            );
            deliveryResponse.setDeliveryTime(delivery.getDeliveryTime());
            deliveryResponse.setRouteInfo(delivery.getRouteInfo());
            deliveryResponse.setUpdatedAt(delivery.getUpdatedAt());
            
            return ResponseEntity.ok(deliveryResponse);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve delivery: " + e.getMessage()));
        }
    }
    
    // Get delivery statistics
    @GetMapping("/stats")
    public ResponseEntity<?> getDeliveryStats(Authentication authentication) {
        try {
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied. Admin role required."));
            }
            
            DeliveryService.DeliveryStats stats = deliveryService.getDeliveryStats();
            return ResponseEntity.ok(stats);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve delivery statistics: " + e.getMessage()));
        }
    }

    // --- ETA & Dispatch helpers ---
    // Simple ETA: return delivery.deliveryTime if set; else now + 1 day as placeholder
    @GetMapping("/eta/{orderId}")
    public ResponseEntity<?> getEtaByOrder(@PathVariable Long orderId) {
        try {
            var deliveryOpt = deliveryService.findByOrder(orderId);
            if (deliveryOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Delivery not found for order"));
            }
            var d = deliveryOpt.get();
            // Eğer Planned Delivery (OrderController'da set edilen) varsa onu kullan
            LocalDateTime planned = d.getDeliveryTime();
            // Varsayılan: yarın 09:00
            LocalDateTime defaultEta = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime eta = planned != null ? planned : defaultEta;
            // naive stopsAhead: parse from routeInfo if available
            Integer stopsAhead = null;
            try {
                if (d.getRouteInfo() != null && d.getRouteInfo().contains("stopsAhead")) {
                    // very naive parsing to avoid adding JSON libs
                    String s = d.getRouteInfo();
                    int i = s.indexOf("stopsAhead");
                    if (i >= 0) {
                        String sub = s.substring(i);
                        String num = sub.replaceAll("[^0-9]", "");
                        if (!num.isEmpty()) stopsAhead = Integer.parseInt(num);
                    }
                }
            } catch (Exception ignored) {}
            // İnsan-dostu metin
            long minutes = java.time.Duration.between(LocalDateTime.now(), eta).toMinutes();
            long days = Math.max(0, minutes / (60 * 24));
            long hours = Math.max(0, (minutes % (60 * 24)) / 60);
            String human = (days > 0 ? days + " gün " : "") + (hours > 0 ? hours + " saat" : (days == 0 ? "<1 saat" : ""));
            DeliveryEtaResponse dto = new DeliveryEtaResponse(d.getOrder().getId(), d.getId(), eta, stopsAhead, d.getDeliveryStatus().name(), d.getDeliveryTime());
            // response'a müdahale etmeden, WhatsApp tarafı için formatlı metin önerisi loglansın
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Failed to get ETA"));
        }
    }

    // Dispatch notify: mark route started (status remains IN_PROGRESS) and return message
    @PostMapping("/dispatch/{deliveryId}")
    public ResponseEntity<?> dispatchDelivery(@PathVariable Long deliveryId) {
        try {
            var dOpt = deliveryService.findById(deliveryId);
            if (dOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Delivery not found"));
            var d = dOpt.get();
            // Update updatedAt via service save path
            deliveryService.planDeliveryRoute(deliveryId, d.getRouteInfo());
            // Notify market via WhatsApp
            // WhatsApp notification removed - no longer needed
            return ResponseEntity.ok(new MessageResponse("Dispatch started"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Failed to dispatch"));
        }
    }
    
    // Helper methods
    private Long getUserIdFromAuthentication(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            return user.getId();
        } catch (Exception e) {
            return null;
        }
    }
    
    private boolean isAdmin(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            return user.getRole() == UserRole.ADMIN;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isSupplier(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            return user.getRole() == UserRole.SUPPLIER;
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
