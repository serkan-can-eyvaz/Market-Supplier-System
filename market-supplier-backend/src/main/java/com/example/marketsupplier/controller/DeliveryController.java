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
    private com.example.marketsupplier.service.WhatsAppService whatsAppService;
    
    @Autowired
    private SupplierService supplierService;
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private com.example.marketsupplier.service.RouteMetricsService routeMetricsService;

    @Autowired
    private com.example.marketsupplier.service.RoutePlanService routePlanService;

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
            if (newStops != null && (oldStops == null || newStops < oldStops)) {
                try {
                    String phone = d.getOrder().getMarket().getPhone();
                    String msg = "🚚 Teslimat güncellendi: sizden önce kalan durak sayısı " + newStops + ".";
                    whatsAppService.sendTextMessage(phone, msg);
                } catch (Exception ignored) {}
            }
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
                                          @RequestHeader("Authorization") String token) {
        try {
            Long userId = getUserIdFromToken(token);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            
            // Check if user is admin or supplier
            if (!isAdmin(token) && !isSupplier(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied. Admin or Supplier role required."));
            }
            
            // If supplier, check if they own the supplier
            if (isSupplier(token) && !supplierService.isSupplierOwner(deliveryRequest.getSupplierId(), userId)) {
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
    
    // Plan delivery route
    @PostMapping("/{deliveryId}/plan-route")
    public ResponseEntity<?> planDeliveryRoute(@PathVariable Long deliveryId,
                                             @Valid @RequestBody RoutePlanRequest routeRequest,
                                             @RequestHeader("Authorization") String token) {
        try {
            Long userId = getUserIdFromToken(token);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            
            // Check if user owns the delivery
            if (!isAdmin(token) && !deliveryService.isDeliveryOwner(deliveryId, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied."));
            }
            
            Delivery delivery = deliveryService.planDeliveryRoute(deliveryId, routeRequest.getRouteInfo());
            
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
            deliveryResponse.setRouteInfo(delivery.getRouteInfo());
            
            return ResponseEntity.ok(deliveryResponse);
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Failed to plan delivery route: " + e.getMessage()));
        }
    }

    // --- Route metrics: persist calculated route totals & fuel estimates ---
    @PostMapping("/route-metrics")
    public ResponseEntity<?> saveRouteMetrics(@RequestBody RouteMetricsRequest req,
                                            @RequestHeader("Authorization") String token) {
        try {
            Long userId = getUserIdFromToken(token);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            // Admin veya ilgili tedarikçi sahibi
            if (!isAdmin(token) && !supplierService.isSupplierOwner(req.getSupplierId(), userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied."));
            }
            var saved = routeMetricsService.saveMetrics(req);
            return ResponseEntity.ok(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Failed to save route metrics: " + e.getMessage()));
        }
    }

    // --- Route plan snapshot (save/load) ---
    @PostMapping("/route-plan")
    public ResponseEntity<?> saveRoutePlan(@RequestHeader("Authorization") String token,
                                           @RequestBody String planJson) {
        try {
            Long userId = getUserIdFromToken(token);
            if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Invalid token"));
            var supplierOpt = supplierService.findByUserId(userId);
            if (supplierOpt.isEmpty()) return ResponseEntity.badRequest().body(new ErrorResponse("Supplier not found"));
            var snap = routePlanService.savePlan(supplierOpt.get().getId(), planJson);
            return ResponseEntity.ok().body(snap.getId());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Failed to save plan: " + e.getMessage()));
        }
    }

    @GetMapping("/route-plan/active")
    public ResponseEntity<?> getActiveRoutePlan(@RequestHeader("Authorization") String token) {
        try {
            Long userId = getUserIdFromToken(token);
            if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Invalid token"));
            var supplierOpt = supplierService.findByUserId(userId);
            if (supplierOpt.isEmpty()) return ResponseEntity.ok().body(null);
            var snap = routePlanService.getActivePlan(supplierOpt.get().getId());
            return ResponseEntity.ok(snap != null ? snap.getPlanJson() : null);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Failed to load plan: " + e.getMessage()));
        }
    }

    @GetMapping("/route-metrics/recent/{supplierId}")
    public ResponseEntity<?> recentRouteMetrics(@PathVariable Long supplierId,
                                                @RequestHeader("Authorization") String token) {
        try {
            Long userId = getUserIdFromToken(token);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            if (!isAdmin(token) && !supplierService.isSupplierOwner(supplierId, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied."));
            }
            return ResponseEntity.ok(routeMetricsService.recentForSupplier(supplierId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Failed to load route metrics: " + e.getMessage()));
        }
    }
    
    // Complete delivery
    @PostMapping("/{deliveryId}/complete")
    public ResponseEntity<?> completeDelivery(@PathVariable Long deliveryId,
                                            @RequestHeader("Authorization") String token) {
        try {
            Long userId = getUserIdFromToken(token);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            
            // Check if user owns the delivery (convert userId to supplierId)
            boolean hasAccess = isAdmin(token);
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
                                                    @RequestHeader("Authorization") String token) {
        try {
            Long userId = getUserIdFromToken(token);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            
            // Check if user is admin or supplier owner
            if (!isAdmin(token) && !supplierService.isSupplierOwner(supplierId, userId)) {
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
    public ResponseEntity<?> getMyDeliveries(@RequestHeader("Authorization") String token,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(defaultValue = "createdAt") String sortBy,
                                           @RequestParam(defaultValue = "desc") String sortDir) {
        try {
            Long userId = getUserIdFromToken(token);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            
            if (!isSupplier(token)) {
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
    public ResponseEntity<?> getAllDeliveries(@RequestHeader("Authorization") String token,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(defaultValue = "createdAt") String sortBy,
                                            @RequestParam(defaultValue = "desc") String sortDir) {
        try {
            Long userId = getUserIdFromToken(token);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            
            // Only admin can access all deliveries
            if (!isAdmin(token)) {
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
                                                               @RequestHeader("Authorization") String token) {
        try {
            Long userId = getUserIdFromToken(token);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            
            // Check if user is admin or supplier owner
            if (!isAdmin(token) && !supplierService.isSupplierOwner(supplierId, userId)) {
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
                                                          @RequestHeader("Authorization") String token) {
        try {
            Long userId = getUserIdFromToken(token);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            
            // Check if user is admin or supplier owner
            if (!isAdmin(token) && !supplierService.isSupplierOwner(supplierId, userId)) {
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
    public ResponseEntity<?> getDeliveryById(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        try {
            Long userId = getUserIdFromToken(token);
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
            boolean hasAccess = isAdmin(token) || 
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
    public ResponseEntity<?> getDeliveryStats(@RequestHeader("Authorization") String token) {
        try {
            if (!isAdmin(token)) {
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
            try {
                String phone = d.getOrder().getMarket().getPhone();
                Integer stops = null;
                if (d.getRouteInfo() != null && d.getRouteInfo().contains("stopsAhead")) {
                    String s = d.getRouteInfo();
                    String num = s.replaceAll(".*stopsAhead[^0-9]*([0-9]+).*", "$1");
                    if (num != null && num.matches("[0-9]+")) stops = Integer.parseInt(num);
                }
                String msg = "🚚 Siparişiniz yola çıktı. Bugün içinde teslim edilecek" +
                    (stops != null ? ", sizden önce " + stops + " durak var." : ".");
                whatsAppService.sendTextMessage(phone, msg);
            } catch (Exception ignored) {}
            return ResponseEntity.ok(new MessageResponse("Dispatch started"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Failed to dispatch"));
        }
    }
    
    // Helper methods
    private Long getUserIdFromToken(String token) {
        try {
            String jwtToken = token.substring(7);
            var userOptional = authService.validateTokenAndGetUser(jwtToken);
            return userOptional.map(User::getId).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
    
    private boolean isAdmin(String token) {
        try {
            String jwtToken = token.substring(7);
            var userOptional = authService.validateTokenAndGetUser(jwtToken);
            return userOptional.isPresent() && userOptional.get().getRole() == UserRole.ADMIN;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isSupplier(String token) {
        try {
            String jwtToken = token.substring(7);
            var userOptional = authService.validateTokenAndGetUser(jwtToken);
            return userOptional.isPresent() && userOptional.get().getRole() == UserRole.SUPPLIER;
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
