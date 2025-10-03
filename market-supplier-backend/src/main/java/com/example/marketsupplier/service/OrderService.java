package com.example.marketsupplier.service;

import com.example.marketsupplier.entity.*;
import com.example.marketsupplier.repository.OrderRepository;
import com.example.marketsupplier.repository.OrderItemRepository;
import com.example.marketsupplier.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@Transactional
public class OrderService {
    

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private OrderItemRepository orderItemRepository;
    
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MarketService marketService;
    
    @Autowired
    private WhatsAppService whatsAppService;

    @Autowired
    private CartService cartService;
    
    // Create new order
    public Order createOrder(Long marketId) {
        // Get market
        Market market = marketService.findById(marketId)
            .orElseThrow(() -> new RuntimeException("Market not found with id: " + marketId));
        
        // Create order
        Order order = new Order(market);
        return orderRepository.save(order);
    }
    
    @Transactional
    public Order createOrderFromCart(Long marketId) {
        Market market = marketService.findById(marketId)
                .orElseThrow(() -> new RuntimeException("Market not found with id: " + marketId));

        List<CartItem> cartItems = cartService.getItems(marketId);
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Cannot create an order from an empty cart.");
        }

        Order order = new Order(market);
        order.setStatus(OrderStatus.PENDING);
        
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem(
                order,
                cartItem.getProductName(),
                cartItem.getQuantity(),
                cartItem.getUnit(),
                cartItem.getPrice()
            );
            order.getItems().add(orderItem);
            total = total.add(cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }
        order.setTotalPrice(total);

        return orderRepository.save(order);
    }

    // Add item to order
    public OrderItem addItemToOrder(Long orderId, String productName, Integer quantity, String unit, BigDecimal price) {
        // Get order
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        
        // Check if order is still pending
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Cannot add items to completed order");
        }
        
        // Create order item
        OrderItem orderItem = new OrderItem(order, productName, quantity, unit, price);
        return orderItemRepository.save(orderItem);
    }

    // Add item to order by productId (name/unit/price come from Product)
    public OrderItem addItemToOrderByProductId(Long orderId, Long productId, Integer quantity) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Cannot add items to completed order");
        }
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        // Stock check
        if (product.getStockQuantity() != null && product.getStockQuantity() < quantity) {
            throw new RuntimeException("Yetersiz stok: " + product.getName());
        }
        OrderItem orderItem = new OrderItem(order, product.getName(), quantity, product.getUnit(), product.getPrice());
        OrderItem saved = orderItemRepository.save(orderItem);
        // Decrease stock
        if (product.getStockQuantity() == null) product.setStockQuantity(0);
        product.setStockQuantity(Math.max(0, product.getStockQuantity() - quantity));
        productRepository.save(product);
        return saved;
    }
    
    // Remove item from order
    public void removeItemFromOrder(Long orderItemId) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
            .orElseThrow(() -> new RuntimeException("Order item not found with id: " + orderItemId));
        
        // Check if order is still pending
        if (orderItem.getOrder().getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Cannot remove items from completed order");
        }
        
        orderItemRepository.delete(orderItem);
    }
    
    // Update order item
    public OrderItem updateOrderItem(Long orderItemId, Integer quantity, BigDecimal price) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
            .orElseThrow(() -> new RuntimeException("Order item not found with id: " + orderItemId));
        
        // Check if order is still pending
        if (orderItem.getOrder().getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Cannot update items in completed order");
        }
        
        orderItem.setQuantity(quantity);
        orderItem.setPrice(price);
        
        return orderItemRepository.save(orderItem);
    }

    @Transactional
    public Order approveOrderWithDeliveryTime(Long orderId, LocalDateTime estimatedDeliveryTime) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be approved.");
        }
        if (order.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot approve an order with no items.");
        }

        order.setStatus(OrderStatus.APPROVED);

        // Yeni Delivery kaydı oluştur veya mevcut olanı güncelle
        Delivery delivery = order.getDelivery();
        if (delivery == null) {
            delivery = new Delivery();
            delivery.setOrder(order);
        }
        delivery.setEstimatedDeliveryTime(estimatedDeliveryTime);
        delivery.setStatus("SCHEDULED");
        
        order.setDelivery(delivery);

        Order savedOrder = orderRepository.save(order);

        // Müşteriye WhatsApp bildirimi gönder
        try {
            String marketPhone = order.getMarket().getPhone();
            String formattedDate = estimatedDeliveryTime.format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm", new java.util.Locale("tr")));
            String message = String.format(
                "Harika haberler! Siparişiniz #%d onaylandı.\n\nTahmini teslimat zamanı: %s.\n\nBizi tercih ettiğiniz için teşekkür ederiz!",
                orderId,
                formattedDate
            );
            whatsAppService.sendTextMessage(marketPhone, message);
        } catch (Exception e) {
            log.error("Failed to send WhatsApp notification for order approval with delivery time", e);
        }

        return savedOrder;
    }

    public Order approveOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Only pending orders can be approved");
        }
        if (orderItemRepository.findByOrderId(orderId).isEmpty()) {
            throw new RuntimeException("Cannot approve order without items");
        }
        order.setStatus(OrderStatus.APPROVED);
        Order savedOrder = orderRepository.save(order);
        
        // Send WhatsApp notification to market
        try {
            String marketPhone = order.getMarket().getPhone();
            String message;
            if (order.getDelivery() != null && order.getDelivery().getEstimatedDeliveryTime() != null) {
                String formattedDate = order.getDelivery().getEstimatedDeliveryTime()
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm", new java.util.Locale("tr")));
                message = String.format("Siparişiniz #%d onaylandı!\nTahmini teslimat tarihi: %s.", orderId, formattedDate);
            } else {
                message = "Siparişiniz #" + orderId + " onaylandı! En kısa sürede teslim edilecektir.";
            }
            whatsAppService.sendTextMessage(marketPhone, message);
        } catch (Exception e) {
            log.error("Failed to send WhatsApp notification for order approval", e);
        }
        
        return savedOrder;
    }

    public Order rejectOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Only pending orders can be rejected");
        }
        order.setStatus(OrderStatus.REJECTED);
        Order savedOrder = orderRepository.save(order);
        
        // Send WhatsApp notification to market
        try {
            String marketPhone = order.getMarket().getPhone();
            String message = "Siparişiniz #" + orderId + " reddedildi. Lütfen tedarikçinizle iletişime geçin.";
            whatsAppService.sendTextMessage(marketPhone, message);
        } catch (Exception e) {
            log.error("Failed to send WhatsApp notification for order rejection", e);
        }
        
        return savedOrder;
    }
    
    // Complete order
    public Order completeOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        
        // Check if order is still pending
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order is already completed");
        }
        
        // Check if order has items
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        if (items.isEmpty()) {
            throw new RuntimeException("Cannot complete order without items");
        }
        
        // Mark order as delivered (ready for supplier assignment)
        order.setStatus(OrderStatus.DELIVERED);
        return orderRepository.save(order);
    }
    
    // Find order by ID
    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }
    
    // Get orders by market
    public List<Order> getOrdersByMarket(Long marketId) {
        return orderRepository.findByMarketIdOrderByCreatedAtDesc(marketId);
    }
    
    // Get orders by market and status
    public List<Order> getOrdersByMarketAndStatus(Long marketId, OrderStatus status) {
        return orderRepository.findByMarketIdAndStatus(marketId, status);
    }
    
    // Get all pending orders (for suppliers)
    public List<Order> getAllPendingOrders() {
        return orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.PENDING);
    }

    public Page<Order> getAllPendingOrders(Pageable pageable) {
        return orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.PENDING, pageable);
    }
    
    // Get orders without delivery assignment
    public List<Order> getOrdersWithoutDelivery() {
        return orderRepository.findOrdersWithoutDelivery();
    }
    
    // Get orders by date range
    public List<Order> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return orderRepository.findByCreatedAtBetween(startDate, endDate);
    }
    
    // Get orders by market and date range
    public List<Order> getOrdersByMarketAndDateRange(Long marketId, LocalDateTime startDate, LocalDateTime endDate) {
        return orderRepository.findByMarketAndCreatedAtBetween(
            marketService.findById(marketId).orElseThrow(() -> new RuntimeException("Market not found")),
            startDate, endDate
        );
    }
    
    // Get order items
    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }
    
    // Calculate order total
    public BigDecimal calculateOrderTotal(Long orderId) {
        BigDecimal total = orderItemRepository.calculateTotalAmountByOrderId(orderId);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    // Get order statistics
    public OrderStats getOrderStats() {
        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
        long deliveredOrders = orderRepository.countByStatus(OrderStatus.DELIVERED);
        List<Object[]> ordersWithTotalAmounts = orderRepository.findOrdersWithTotalAmounts();
        
        return new OrderStats(totalOrders, pendingOrders, deliveredOrders, ordersWithTotalAmounts);
    }
    
    // Get order statistics for market
    public OrderStats getOrderStatsForMarket(Long marketId) {
        Market market = marketService.findById(marketId)
            .orElseThrow(() -> new RuntimeException("Market not found with id: " + marketId));
        
        long totalOrders = orderRepository.countByMarket(market);
        long pendingOrders = orderRepository.countByMarketIdAndStatus(marketId, OrderStatus.PENDING);
        long deliveredOrders = orderRepository.countByMarketIdAndStatus(marketId, OrderStatus.DELIVERED);
        
        return new OrderStats(totalOrders, pendingOrders, deliveredOrders, null);
    }
    
    // Validate order ownership
    public boolean isOrderOwner(Long orderId, Long marketId) {
        Optional<Order> order = orderRepository.findById(orderId);
        return order.isPresent() && order.get().getMarket().getId().equals(marketId);
    }
    
    // Get most ordered products
    public List<Object[]> getMostOrderedProducts() {
        return orderItemRepository.findMostOrderedProducts();
    }
    
    // Get average price by product
    public BigDecimal getAveragePriceByProduct(String productName) {
        BigDecimal avgPrice = orderItemRepository.findAveragePriceByProductName(productName);
        return avgPrice != null ? avgPrice : BigDecimal.ZERO;
    }
    
    // Inner class for order statistics
    public static class OrderStats {
        private final long totalOrders;
        private final long pendingOrders;
        private final long deliveredOrders;
        private final List<Object[]> ordersWithTotalAmounts;
        
        public OrderStats(long totalOrders, long pendingOrders, long deliveredOrders, List<Object[]> ordersWithTotalAmounts) {
            this.totalOrders = totalOrders;
            this.pendingOrders = pendingOrders;
            this.deliveredOrders = deliveredOrders;
            this.ordersWithTotalAmounts = ordersWithTotalAmounts;
        }
        
        // Getters
        public long getTotalOrders() { return totalOrders; }
        public long getPendingOrders() { return pendingOrders; }
        public long getDeliveredOrders() { return deliveredOrders; }
        public List<Object[]> getOrdersWithTotalAmounts() { return ordersWithTotalAmounts; }
    }
    
    // Get all orders
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
    
    // Get all orders with pagination
    public Page<Order> getAllOrdersPaginated(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }
    
    // Clear order items
    public void clearOrderItems(Long orderId) {
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        orderItemRepository.deleteAll(items);
    }
    
    // Delete order
    public void deleteOrder(Long orderId) {
        // First delete all order items
        clearOrderItems(orderId);
        // Then delete the order
        orderRepository.deleteById(orderId);
    }

    public Optional<Order> findLatestActiveOrderByMarket(Long marketId) {
        // Find orders for the market that are in a non-final state, ordered by most recent first
        List<Order> orders = orderRepository.findLatestActiveOrderByMarketWithDelivery(
            marketId, 
            List.of(OrderStatus.APPROVED, OrderStatus.SHIPPED)
        );
        // Return the first one if the list is not empty
        return orders.stream().findFirst();
    }

    public List<Order> findAllCompletedOrdersByMarket(Long marketId) {
        // Find all orders for the market that are in a final, delivered state
        return orderRepository.findByMarketIdAndStatusInOrderByCreatedAtDesc(
            marketId,
            List.of(OrderStatus.DELIVERED, OrderStatus.COMPLETED)
        );
    }

    public List<Order> findPendingOrdersByMarket(Long marketId) {
        // Find all pending orders for the market
        return orderRepository.findByMarketIdAndStatusOrderByCreatedAtDesc(marketId, OrderStatus.PENDING);
    }

    /**
     * Akıllı sipariş durumu sorgulama - AI-driven yaklaşım
     * 1. Onaylanmış ama teslim edilmemiş sipariş varsa sadece onu göster
     * 2. Bekleyen siparişler varsa son 5'ini göster
     * 3. Teslim edilen sipariş varsa en son teslim edileni göster
     */
    public String getSmartOrderStatus(Long marketId) {
        // 1. Önce onaylanmış ama teslim edilmemiş siparişleri kontrol et
        List<Order> approvedNotDelivered = orderRepository.findApprovedButNotDeliveredOrders(
            marketId, 
            List.of(OrderStatus.APPROVED, OrderStatus.SHIPPED)
        );
        
        if (!approvedNotDelivered.isEmpty()) {
            Order order = approvedNotDelivered.get(0); // En son onaylanmış
            StringBuilder statusMessage = new StringBuilder();
            statusMessage.append("✅ **Siparişiniz #").append(order.getId()).append(" onaylandı!**\n\n");
            statusMessage.append("💰 **Tutar:** ").append(String.format("%.2f TL", order.getTotalPrice())).append("\n");
            
            // Teslimat tarihi kontrol et
            Delivery delivery = order.getDelivery();
            if (delivery != null && delivery.getEstimatedDeliveryTime() != null) {
                String formattedDate = delivery.getEstimatedDeliveryTime().format(
                    DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm", new Locale("tr"))
                );
                statusMessage.append("🚚 **Tahmini teslimat tarihi:** ").append(formattedDate);
            } else {
                statusMessage.append("🚚 **Durum:** Onaylandı, teslimat tarihi belirleniyor");
            }
            
            return statusMessage.toString();
        }
        
        // 2. Bekleyen siparişleri kontrol et (son 5)
        List<Order> pendingOrders = orderRepository.findByMarketIdAndStatusOrderByCreatedAtDesc(marketId, OrderStatus.PENDING);
        if (!pendingOrders.isEmpty()) {
            StringBuilder statusMessage = new StringBuilder();
            statusMessage.append("📋 **Bekleyen Siparişleriniz:**\n\n");
            
            int count = Math.min(5, pendingOrders.size()); // Son 5
            for (int i = 0; i < count; i++) {
                Order order = pendingOrders.get(i);
                statusMessage.append("🔄 **Sipariş #").append(order.getId()).append("** - ")
                           .append(String.format("%.2f TL", order.getTotalPrice())).append("\n");
                statusMessage.append("⏳ **Durum:** Tedarikçi onayı bekleniyor\n");
                statusMessage.append("📅 **Tarih:** ")
                           .append(order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm", new Locale("tr"))))
                           .append("\n\n");
            }
            
            if (pendingOrders.size() > 5) {
                statusMessage.append("... ve ").append(pendingOrders.size() - 5).append(" sipariş daha");
            }
            
            return statusMessage.toString();
        }
        
        // 3. Teslim edilen siparişleri kontrol et (en son teslim edilen)
        List<Order> deliveredOrders = orderRepository.findDeliveredOrdersByMarket(marketId, OrderStatus.DELIVERED);
        if (!deliveredOrders.isEmpty()) {
            Order order = deliveredOrders.get(0); // En son teslim edilen
            StringBuilder statusMessage = new StringBuilder();
            statusMessage.append("✅ **Son Teslim Edilen Siparişiniz:**\n\n");
            statusMessage.append("📦 **Sipariş #").append(order.getId()).append("** - ")
                       .append(String.format("%.2f TL", order.getTotalPrice())).append("\n");
            statusMessage.append("🎉 **Durum:** Teslim edildi\n");
            statusMessage.append("📅 **Teslimat Tarihi:** ")
                       .append(order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm", new Locale("tr"))));
            
            return statusMessage.toString();
        }
        
        // Hiç sipariş yoksa
        return "Henüz siparişiniz bulunmuyor. Yeni sipariş vermek için ürünlerimizi inceleyebilirsiniz.";
    }
    
    // Admin için sipariş verilen toplam ürün (kalem) sayısını getir
    public Long getTotalOrderItems() {
        return orderRepository.countTotalOrderItems();
    }
}
