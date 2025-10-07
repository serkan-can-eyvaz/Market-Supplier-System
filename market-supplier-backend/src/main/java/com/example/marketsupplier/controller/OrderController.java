package com.example.marketsupplier.controller;

import com.example.marketsupplier.dto.*;
import com.example.marketsupplier.entity.*;
import com.example.marketsupplier.service.MarketService;
import com.example.marketsupplier.service.OrderService;
import com.example.marketsupplier.service.OrderPdfService;
import com.example.marketsupplier.service.DeliveryService;
import com.example.marketsupplier.service.SupplierService;
import com.example.marketsupplier.service.UserService;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import java.net.MalformedURLException;


@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private MarketService marketService;
    
    
    @Autowired
    private OrderPdfService orderPdfService;
    
    @Autowired
    private DeliveryService deliveryService;
    
    @Autowired
    private SupplierService supplierService;
    
    @Autowired
    private UserService userService;
    
    // Create new order with items
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request,
                                       Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            
            // Get user's market or create one if it doesn't exist
            var marketOptional = marketService.findByUserId(userId);
            if (marketOptional.isEmpty()) {
                // Create a default market for the user
                User user = (User) authentication.getPrincipal();
                var market = marketService.createMarket(
                    user.getId(),
                    user.getName() + " Market",
                    "Adres bilgisi güncellenmeli",
                    "Telefon bilgisi güncellenmeli"
                );
                marketOptional = Optional.of(market);
            }
            
            Order order = orderService.createOrder(marketOptional.get().getId());
            
            // Add order items (prefer productId if provided)
            for (OrderItemRequest itemRequest : request.getOrderItems()) {
                if (itemRequest.getProductId() != null) {
                    orderService.addItemToOrderByProductId(order.getId(), itemRequest.getProductId(), itemRequest.getQuantity());
                } else {
                    orderService.addItemToOrder(
                        order.getId(),
                        itemRequest.getProductName(),
                        itemRequest.getQuantity(),
                        itemRequest.getUnit(),
                        itemRequest.getPrice()
                    );
                }
            }
            
            OrderResponse orderResponse = new OrderResponse(
                order.getId(),
                order.getMarket().getId(),
                order.getMarket().getName(),
                order.getMarket().getAddress(),
                order.getStatus(),
                order.getCreatedAt()
            );
            orderResponse.setMarketPhone(order.getMarket().getPhone());
            
            // Add order items to response
            List<OrderItem> orderItems = orderService.getOrderItems(order.getId());
            List<OrderItemResponse> itemResponses = orderItems.stream()
                .map(item -> new OrderItemResponse(
                    item.getId(),
                    item.getProductName(),
                    item.getQuantity(),
                    item.getUnit(),
                    item.getPrice()
                ))
                .collect(Collectors.toList());
            orderResponse.setOrderItems(itemResponses);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(orderResponse);
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Failed to create order: " + e.getMessage()));
        }
    }
    
    // Add item to order
    @PostMapping("/{orderId}/items")
    public ResponseEntity<?> addItemToOrder(@PathVariable Long orderId,
                                          @Valid @RequestBody OrderItemRequest itemRequest,
                                          Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            
            // Check if user owns the order
            var orderOptional = orderService.findById(orderId);
            if (orderOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            if (!orderService.isOrderOwner(orderId, orderOptional.get().getMarket().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied."));
            }
            
            OrderItem orderItem;
            if (itemRequest.getProductId() != null) {
                orderItem = orderService.addItemToOrderByProductId(orderId, itemRequest.getProductId(), itemRequest.getQuantity());
            } else {
                orderItem = orderService.addItemToOrder(
                    orderId,
                    itemRequest.getProductName(),
                    itemRequest.getQuantity(),
                    itemRequest.getUnit(),
                    itemRequest.getPrice()
                );
            }
            
            OrderItemResponse itemResponse = new OrderItemResponse(
                orderItem.getId(),
                orderItem.getProductName(),
                orderItem.getQuantity(),
                orderItem.getUnit(),
                orderItem.getPrice()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(itemResponse);
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Failed to add item to order: " + e.getMessage()));
        }
    }
    
    // Download order PDF (supplier/admin; market kendi siparişini indirebilir)
    @GetMapping("/{orderId}/pdf")
    @CrossOrigin(origins = "*", exposedHeaders = {"Content-Disposition"})
    public ResponseEntity<?> downloadOrderPdf(@PathVariable Long orderId,
                                            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            var orderOpt = orderService.findById(orderId);
            if (orderOpt.isEmpty()) return ResponseEntity.notFound().build();
            var order = orderOpt.get();

            boolean hasAccess = isAdmin(authentication) || isSupplier(authentication) || marketService.isMarketOwner(order.getMarket().getId(), userId);
            if (!hasAccess) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied."));
            }

            try {
                byte[] pdf = orderPdfService.generateOrderPdf(order);
                return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename=order-" + orderId + ".pdf")
                    .body(pdf);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to generate PDF: " + e.getMessage()));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to generate PDF: " + e.getMessage()));
        }
    }
    
    // Update order item
    @PutMapping("/items/{itemId}")
    public ResponseEntity<?> updateOrderItem(@PathVariable Long itemId,
                                           @Valid @RequestBody OrderItemRequest itemRequest,
                                           Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            
            OrderItem orderItem = orderService.updateOrderItem(
                itemId,
                itemRequest.getQuantity(),
                itemRequest.getPrice()
            );
            
            OrderItemResponse itemResponse = new OrderItemResponse(
                orderItem.getId(),
                orderItem.getProductName(),
                orderItem.getQuantity(),
                orderItem.getUnit(),
                orderItem.getPrice()
            );
            
            return ResponseEntity.ok(itemResponse);
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Failed to update order item: " + e.getMessage()));
        }
    }
    
    // Remove item from order
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<?> removeItemFromOrder(@PathVariable Long itemId,
                                               Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            
            orderService.removeItemFromOrder(itemId);
            return ResponseEntity.ok(new MessageResponse("Item removed from order successfully"));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Failed to remove item from order: " + e.getMessage()));
        }
    }
    
    // Complete order
    @PostMapping("/{orderId}/complete")
    public ResponseEntity<?> completeOrder(@PathVariable Long orderId,
                                         Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            
            // Check if user owns the order
            var orderOptional = orderService.findById(orderId);
            if (orderOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            if (!orderService.isOrderOwner(orderId, orderOptional.get().getMarket().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied."));
            }
            
            Order completedOrder = orderService.completeOrder(orderId);
            
            OrderResponse orderResponse = new OrderResponse(
                completedOrder.getId(),
                completedOrder.getMarket().getId(),
                completedOrder.getMarket().getName(),
                completedOrder.getMarket().getAddress(),
                completedOrder.getStatus(),
                completedOrder.getCreatedAt()
            );
            
            return ResponseEntity.ok(orderResponse);
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Failed to complete order: " + e.getMessage()));
        }
    }

    // Supplier/Admin approves order
    @PostMapping("/{orderId}/approve")
    public ResponseEntity<?> approve(@PathVariable Long orderId,
                                     Authentication authentication) {
        try {
            if (!isSupplier(authentication) && !isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied."));
            }
            Order order = orderService.approveOrder(orderId);
            // After approval, auto create delivery for current supplier (only if not already assigned)
            try {
                Long userId = getUserIdFromAuthentication(authentication);
                if (userId != null) {
                    supplierService.findByUserId(userId).ifPresent(s -> {
                        try {
                            // Avoid duplicate delivery creation
                            if (order.getDelivery() == null) {
                                deliveryService.createDelivery(order.getId(), s.getId());
                            }
                        } catch (RuntimeException ignored) {}
                    });
                }
            } catch (Exception ignored) {}
            return ResponseEntity.ok(new MessageResponse("Order approved"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // Supplier/Admin approves order and sets delivery time
    @PostMapping("/{orderId}/approve-with-delivery")
    public ResponseEntity<?> approveWithDelivery(@PathVariable Long orderId,
                                                   @RequestBody DeliveryTimeRequest request,
                                                   Authentication authentication) {
        try {
            if (!isSupplier(authentication) && !isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied."));
            }
            orderService.approveOrderWithDeliveryTime(orderId, request.getEstimatedDeliveryTime());
            return ResponseEntity.ok(new MessageResponse("Order approved and delivery time set. Customer notified."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // Supplier sets planned delivery date for created/approved order
    @PostMapping("/{orderId}/planned-date")
    public ResponseEntity<?> setPlannedDeliveryDate(@PathVariable Long orderId,
                                                    @RequestParam String dateTimeIso,
                                                    Authentication authentication) {
        try {
            if (!isSupplier(authentication) && !isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied."));
            }
            var dOpt = deliveryService.findByOrder(orderId);
            var d = dOpt.orElseGet(() -> {
                // If no delivery yet, create one for current supplier
                Long userId = getUserIdFromAuthentication(authentication);
                var supplierOpt = supplierService.findByUserId(userId);
                if (supplierOpt.isPresent()) {
                    return deliveryService.createDelivery(orderId, supplierOpt.get().getId());
                }
                return null;
            });
            if (d == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Delivery not found"));
            }
            // yyyy-MM-dd'T'HH:mm veya yyyy-MM-dd'T'HH:mm:ss formatını destekle
            java.time.format.DateTimeFormatter formatter = new java.time.format.DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm")
                .optionalStart().appendPattern(":ss").optionalEnd()
                .toFormatter();
            LocalDateTime planned = LocalDateTime.parse(dateTimeIso, formatter);
            d.setDeliveryTime(planned);
            // Tahmini teslimat zaman olarak da kaydet
            d.setEstimatedDeliveryTime(planned);
            // save via existing path
            deliveryService.planDeliveryRoute(d.getId(), d.getRouteInfo());
            return ResponseEntity.ok(new MessageResponse("Planned date set"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Failed to set planned date"));
        }
    }

    // Supplier/Admin rejects order
    @PostMapping("/{orderId}/reject")
    public ResponseEntity<?> reject(@PathVariable Long orderId,
                                    Authentication authentication) {
        try {
            if (!isSupplier(authentication) && !isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied."));
            }
            Order order = orderService.rejectOrder(orderId);
            return ResponseEntity.ok(new MessageResponse("Order rejected"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
    
    // Get current user's market orders
    @GetMapping("/market")
    public ResponseEntity<?> getMarketOrders(Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            
            // Get user's market or create one if it doesn't exist
            var marketOptional = marketService.findByUserId(userId);
            if (marketOptional.isEmpty()) {
                // Create a default market for the user
                User user = (User) authentication.getPrincipal();
                var market = marketService.createMarket(
                    user.getId(),
                    user.getName() + " Market",
                    "Adres bilgisi güncellenmeli",
                    "Telefon bilgisi güncellenmeli"
                );
                marketOptional = Optional.of(market);
            }
            
            List<Order> orders = orderService.getOrdersByMarket(marketOptional.get().getId());
            List<OrderResponse> orderResponses = orders.stream()
                .map(order -> {
                    OrderResponse response = new OrderResponse(
                        order.getId(),
                        order.getMarket().getId(),
                        order.getMarket().getName(),
                        order.getMarket().getAddress(),
                        order.getStatus(),
                        order.getCreatedAt()
                    );
                    response.setMarketPhone(order.getMarket().getPhone());
                    
                    // Add order items
                    List<OrderItem> orderItems = orderService.getOrderItems(order.getId());
                    List<OrderItemResponse> itemResponses = orderItems.stream()
                        .map(item -> new OrderItemResponse(
                            item.getId(),
                            item.getProductName(),
                            item.getQuantity(),
                            item.getUnit(),
                            item.getPrice()
                        ))
                        .collect(Collectors.toList());
                    response.setOrderItems(itemResponses);
                    
                    // Calculate total amount
                    BigDecimal totalAmount = orderService.calculateOrderTotal(order.getId());
                    response.setTotalAmount(totalAmount);
                    response.setItemCount((long) itemResponses.size());
                    
                    return response;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(orderResponses);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve orders: " + e.getMessage()));
        }
    }
    
    // Get orders by market ID (for admin)
    @GetMapping("/market/{marketId}")
    public ResponseEntity<?> getOrdersByMarket(@PathVariable Long marketId,
                                             Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            
            // Check if user is admin or market owner
            if (!isAdmin(authentication) && !marketService.isMarketOwner(marketId, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied."));
            }
            
            List<Order> orders = orderService.getOrdersByMarket(marketId);
            List<OrderResponse> orderResponses = orders.stream()
                .map(order -> {
                    OrderResponse response = new OrderResponse(
                        order.getId(),
                        order.getMarket().getId(),
                        order.getMarket().getName(),
                        order.getMarket().getAddress(),
                        order.getStatus(),
                        order.getCreatedAt()
                    );
                    response.setMarketPhone(order.getMarket().getPhone());
                    
                    // Calculate total amount
                    BigDecimal totalAmount = orderService.calculateOrderTotal(order.getId());
                    response.setTotalAmount(totalAmount);
                    
                    // Set item count
                    long itemCount = orderService.getOrderItems(order.getId()).size();
                    response.setItemCount(itemCount);
                    
                    return response;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(orderResponses);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve orders: " + e.getMessage()));
        }
    }
    
    // Get all orders (for suppliers and admin) with pagination
    @GetMapping
    public ResponseEntity<?> getAllOrders(Authentication authentication,
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
            
            Page<Order> orderPage = orderService.getAllOrdersPaginated(pageable);
            
            List<OrderResponse> orderResponses = orderPage.getContent().stream()
                .map(order -> {
                    OrderResponse response = new OrderResponse(
                        order.getId(),
                        order.getMarket().getId(),
                        order.getMarket().getName(),
                        order.getMarket().getAddress(),
                        order.getStatus(),
                        order.getCreatedAt()
                    );
                    response.setMarketPhone(order.getMarket().getPhone());
                    
                    // Add order items
                    List<OrderItem> orderItems = orderService.getOrderItems(order.getId());
                    List<OrderItemResponse> itemResponses = orderItems.stream()
                        .map(item -> new OrderItemResponse(
                            item.getId(),
                            item.getProductName(),
                            item.getQuantity(),
                            item.getUnit(),
                            item.getPrice()
                        ))
                        .collect(Collectors.toList());
                    response.setOrderItems(itemResponses);
                    
                    // Calculate total amount
                    BigDecimal totalAmount = orderService.calculateOrderTotal(order.getId());
                    response.setTotalAmount(totalAmount);
                    response.setItemCount((long) itemResponses.size());
                    
                    return response;
                })
                .collect(Collectors.toList());
            
            PaginatedResponse<OrderResponse> paginatedResponse = new PaginatedResponse<>(
                orderResponses,
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages(),
                orderPage.isFirst(),
                orderPage.isLast()
            );
            
            return ResponseEntity.ok(paginatedResponse);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve orders: " + e.getMessage()));
        }
    }
    
    // Get all pending orders (for suppliers) - paginated
    @GetMapping("/pending")
    public ResponseEntity<?> getAllPendingOrders(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        try {
            if (!isSupplier(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied. Supplier role required."));
            }

            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            Page<Order> orderPage = orderService.getAllPendingOrders(pageable);

            List<OrderResponse> orderResponses = orderPage.getContent().stream()
                .map(order -> {
                    OrderResponse response = new OrderResponse(
                        order.getId(),
                        order.getMarket().getId(),
                        order.getMarket().getName(),
                        order.getMarket().getAddress(),
                        order.getStatus(),
                        order.getCreatedAt()
                    );
                    response.setMarketPhone(order.getMarket().getPhone());
                    BigDecimal totalAmount = orderService.calculateOrderTotal(order.getId());
                    response.setTotalAmount(totalAmount);
                    List<OrderItem> orderItems = orderService.getOrderItems(order.getId());
                    response.setOrderItems(orderItems.stream()
                        .map(item -> new OrderItemResponse(
                            item.getId(),
                            item.getProductName(),
                            item.getQuantity(),
                            item.getUnit(),
                            item.getPrice()
                        ))
                        .collect(Collectors.toList()));
                    response.setItemCount((long) orderItems.size());
                    return response;
                })
                .collect(Collectors.toList());

            PaginatedResponse<OrderResponse> paginatedResponse = new PaginatedResponse<>(
                orderResponses,
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages(),
                orderPage.isFirst(),
                orderPage.isLast()
            );

            return ResponseEntity.ok(paginatedResponse);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve pending orders: " + e.getMessage()));
        }
    }
    
    // Get order by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            
            var orderOptional = orderService.findById(id);
            if (orderOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Order order = orderOptional.get();
            
            // Check access permissions
            boolean hasAccess = isAdmin(authentication) || 
                              marketService.isMarketOwner(order.getMarket().getId(), userId) ||
                              isSupplier(authentication);
            
            if (!hasAccess) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied."));
            }
            
            OrderResponse orderResponse = new OrderResponse(
                order.getId(),
                order.getMarket().getId(),
                order.getMarket().getName(),
                order.getMarket().getAddress(),
                order.getStatus(),
                order.getCreatedAt()
            );
            orderResponse.setMarketPhone(order.getMarket().getPhone());
            
            // Add order items
            List<OrderItem> orderItems = orderService.getOrderItems(order.getId());
            List<OrderItemResponse> itemResponses = orderItems.stream()
                .map(item -> new OrderItemResponse(
                    item.getId(),
                    item.getProductName(),
                    item.getQuantity(),
                    item.getUnit(),
                    item.getPrice()
                ))
                .collect(Collectors.toList());
            
            orderResponse.setOrderItems(itemResponses);
            
            // Calculate total amount
            BigDecimal totalAmount = orderService.calculateOrderTotal(order.getId());
            orderResponse.setTotalAmount(totalAmount);
            orderResponse.setItemCount((long) itemResponses.size());
            
            return ResponseEntity.ok(orderResponse);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve order: " + e.getMessage()));
        }
    }
    
    // Update order
    @PutMapping("/{orderId}")
    public ResponseEntity<?> updateOrder(@PathVariable Long orderId,
                                       @RequestBody UpdateOrderRequest request,
                                       Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            
            // Check if user owns the order
            var orderOptional = orderService.findById(orderId);
            if (orderOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Order order = orderOptional.get();
            if (!orderService.isOrderOwner(orderId, order.getMarket().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied."));
            }
            
            // Clear existing items and add new ones (prefer productId)
            orderService.clearOrderItems(orderId);
            for (OrderItemRequest itemRequest : request.getOrderItems()) {
                if (itemRequest.getProductId() != null) {
                    orderService.addItemToOrderByProductId(orderId, itemRequest.getProductId(), itemRequest.getQuantity());
                } else {
                    orderService.addItemToOrder(
                        orderId,
                        itemRequest.getProductName(),
                        itemRequest.getQuantity(),
                        itemRequest.getUnit(),
                        itemRequest.getPrice()
                    );
                }
            }
            
            // Get updated order
            orderOptional = orderService.findById(orderId);
            order = orderOptional.get();
            
            OrderResponse orderResponse = new OrderResponse(
                order.getId(),
                order.getMarket().getId(),
                order.getMarket().getName(),
                order.getMarket().getAddress(),
                order.getStatus(),
                order.getCreatedAt()
            );
            orderResponse.setMarketPhone(order.getMarket().getPhone());
            
            // Add order items to response
            List<OrderItem> orderItems = orderService.getOrderItems(order.getId());
            List<OrderItemResponse> itemResponses = orderItems.stream()
                .map(item -> new OrderItemResponse(
                    item.getId(),
                    item.getProductName(),
                    item.getQuantity(),
                    item.getUnit(),
                    item.getPrice()
                ))
                .collect(Collectors.toList());
            orderResponse.setOrderItems(itemResponses);
            
            return ResponseEntity.ok(orderResponse);
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Failed to update order: " + e.getMessage()));
        }
    }
    
    // Delete order
    @DeleteMapping("/{orderId}")
    public ResponseEntity<?> deleteOrder(@PathVariable Long orderId,
                                       Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            
            // Check if user owns the order
            var orderOptional = orderService.findById(orderId);
            if (orderOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Order order = orderOptional.get();
            if (!orderService.isOrderOwner(orderId, order.getMarket().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied."));
            }
            
            orderService.deleteOrder(orderId);
            return ResponseEntity.ok(new MessageResponse("Order deleted successfully"));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Failed to delete order: " + e.getMessage()));
        }
    }
    
    // Get order status by phone number (for AI agent)
    @GetMapping("/status/{phone}")
    public ResponseEntity<?> getOrderStatusByPhone(@PathVariable String phone) {
        try {
            // Find market by phone
            var marketOptional = marketService.findByPhoneNormalized(phone);
            if (marketOptional.isEmpty()) {
                return ResponseEntity.ok().body(null); // No market found
            }
            
            // Get the most recent order for this market
            List<Order> orders = orderService.getOrdersByMarket(marketOptional.get().getId());
            if (orders.isEmpty()) {
                return ResponseEntity.ok().body(null); // No orders found
            }
            
            // Get the most recent order
            Order latestOrder = orders.get(0);
            
            // Calculate total amount
            BigDecimal totalAmount = orderService.calculateOrderTotal(latestOrder.getId());
            
            // Get order items
            List<OrderItem> orderItems = orderService.getOrderItems(latestOrder.getId());
            List<OrderItemResponse> itemResponses = orderItems.stream()
                .map(item -> new OrderItemResponse(
                    item.getId(),
                    item.getProductName(),
                    item.getQuantity(),
                    item.getUnit(),
                    item.getPrice()
                ))
                .collect(Collectors.toList());
            
            // Create response
            OrderResponse orderResponse = new OrderResponse(
                latestOrder.getId(),
                latestOrder.getMarket().getId(),
                latestOrder.getMarket().getName(),
                latestOrder.getMarket().getAddress(),
                latestOrder.getStatus(),
                latestOrder.getCreatedAt()
            );
            orderResponse.setMarketPhone(latestOrder.getMarket().getPhone());
            orderResponse.setOrderItems(itemResponses);
            orderResponse.setTotalAmount(totalAmount);
            orderResponse.setItemCount((long) itemResponses.size());
            
            return ResponseEntity.ok(orderResponse);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve order status: " + e.getMessage()));
        }
    }
    
    // Get order statistics
    @GetMapping("/stats")
    public ResponseEntity<?> getOrderStats(Authentication authentication) {
        try {
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied. Admin role required."));
            }
            
            OrderService.OrderStats stats = orderService.getOrderStats();
            return ResponseEntity.ok(stats);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve order statistics: " + e.getMessage()));
        }
    }

    // Endpoint to serve temporary PDF files
    @GetMapping("/history/{fileName}")
    public ResponseEntity<Resource> downloadHistoryPdf(@PathVariable String fileName) {
        try {
            Path file = Paths.get(System.getProperty("java.io.tmpdir")).resolve(fileName);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                throw new RuntimeException("Could not read the file!");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error: " + e.getMessage());
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

    // Admin için sipariş verilen toplam ürün (kalem) sayısını getir
    @GetMapping("/total-items")
    public ResponseEntity<?> getTotalOrderItems(Authentication authentication) {
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
            
            // Sadece ADMIN rolü erişebilir
            if (!user.getRole().name().equals("ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied. Admin role required.");
            }
            
            Long totalItems = orderService.getTotalOrderItems();
            return ResponseEntity.ok(Map.of("totalItems", totalItems));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving total order items: " + e.getMessage());
        }
    }

    // Inner DTO for delivery time request
    public static class DeliveryTimeRequest {
        private LocalDateTime estimatedDeliveryTime;

        public LocalDateTime getEstimatedDeliveryTime() {
            return estimatedDeliveryTime;
        }

        public void setEstimatedDeliveryTime(LocalDateTime estimatedDeliveryTime) {
            this.estimatedDeliveryTime = estimatedDeliveryTime;
        }
    }
}
