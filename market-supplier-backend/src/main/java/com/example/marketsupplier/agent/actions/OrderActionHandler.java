package com.example.marketsupplier.agent.actions;

import com.example.marketsupplier.entity.Cart;
import com.example.marketsupplier.entity.CartItem;
import com.example.marketsupplier.entity.Market;
import com.example.marketsupplier.entity.Order;
import com.example.marketsupplier.entity.OrderItem;
import com.example.marketsupplier.entity.OrderStatus;
import com.example.marketsupplier.repository.CartItemRepository;
import com.example.marketsupplier.repository.CartRepository;
import com.example.marketsupplier.repository.OrderRepository;
import com.example.marketsupplier.service.MarketService;
import com.example.marketsupplier.agent.context.ContextManager.CartContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.example.marketsupplier.service.CustomerContext;

@Component
public class OrderActionHandler implements ActionHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderActionHandler.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private MarketService marketService;

    @Override
    public String getActionName() { return "order"; }

    @Override
    public boolean canHandle(String action) {
        return action != null && (action.equals("order.confirm") || action.equals("order.cancel"));
    }

    @Override
    public String handle(String phone, String text, CustomerContext context) {
        if (text == null) text = "";
        if (text.toLowerCase(java.util.Locale.ROOT).contains("iptal")) {
            return cancelOrder(phone, context);
        }
        return confirmOrder(phone, context);
    }

    @Transactional
    public String confirmOrder(String phone, CustomerContext context) {
        try {
            Long marketId = resolveMarketIdByPhone(phone);
            if (marketId == null) return "Numaranız sisteme kayıtlı değil. Lütfen önce kaydınızı tamamlayın.";

            // Cart'ı bul
            List<CartItem> cartItems = cartItemRepository.findByMarketId(marketId);
            if (cartItems.isEmpty()) {
                return "Sepetiniz boş. Önce ürün ekleyin.";
            }

            // Order oluştur
            Optional<Market> marketOpt = marketService.findById(marketId);
            if (marketOpt.isEmpty()) {
                return "Market bilgisi bulunamadı.";
            }

            Order order = new Order(marketOpt.get());
            order.setStatus(OrderStatus.PENDING);
            order = orderRepository.save(order);

            // CartItem'ları OrderItem'a dönüştür
            BigDecimal totalPrice = BigDecimal.ZERO;
            for (CartItem cartItem : cartItems) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                // Product ID is not stored in OrderItem, only product name
                orderItem.setProductName(cartItem.getProductName());
                orderItem.setQuantity(cartItem.getQuantity());
                orderItem.setUnit(cartItem.getUnit());
                orderItem.setPrice(cartItem.getPrice());
                order.getItems().add(orderItem);

                BigDecimal itemTotal = cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
                totalPrice = totalPrice.add(itemTotal);
            }

            order.setTotalPrice(totalPrice);
            orderRepository.save(order);

            // Cart'ı temizle
            cartItemRepository.deleteByMarketId(marketId);

            log.info("Order created: orderId={}, marketId={}, totalPrice={}", order.getId(), marketId, totalPrice);
            return String.format("✅ Siparişiniz oluşturuldu!\n📋 Sipariş No: #%d\n💰 Toplam: %.2f TL\n\nSipariş durumunu takip etmek için 'sipariş durumu' yazabilirsiniz.", 
                               order.getId(), totalPrice);

        } catch (Exception e) {
            log.error("Failed to confirm order for phone: {}", phone, e);
            return "Sipariş oluşturulurken hata oluştu. Lütfen tekrar deneyin.";
        }
    }

    @Transactional
    public String cancelOrder(String phone, CustomerContext context) {
        try {
            Long marketId = resolveMarketIdByPhone(phone);
            if (marketId == null) return "Numaranız sisteme kayıtlı değil.";

            // Pending order'ları iptal et
            List<Order> pendingOrders = orderRepository.findByMarketIdAndStatusOrderByCreatedAtDesc(marketId, OrderStatus.PENDING);
            if (pendingOrders.isEmpty()) {
                return "İptal edilecek bekleyen sipariş bulunmuyor.";
            }

            int cancelledCount = 0;
            for (Order order : pendingOrders) {
                order.setStatus(OrderStatus.CANCELED);
                orderRepository.save(order);
                cancelledCount++;
            }

            log.info("Cancelled {} orders for marketId: {}", cancelledCount, marketId);
            return String.format("✅ %d sipariş iptal edildi.", cancelledCount);

        } catch (Exception e) {
            log.error("Failed to cancel order for phone: {}", phone, e);
            return "Sipariş iptal edilirken hata oluştu. Lütfen tekrar deneyin.";
        }
    }

    public String getOrderStatus(String phone, CartContext context) {
        try {
            Long marketId = resolveMarketIdByPhone(phone);
            if (marketId == null) return "Numaranız sisteme kayıtlı değil.";

            List<Order> recentOrders = orderRepository.findByMarketIdAndCreatedAtAfterOrderByCreatedAtDesc(
                marketId, LocalDateTime.now().minusDays(7));

            if (recentOrders.isEmpty()) {
                return "Son 7 gün içinde sipariş bulunmuyor.";
            }

            StringBuilder sb = new StringBuilder("📋 Sipariş Durumları:\n\n");
            for (Order order : recentOrders) {
                sb.append(String.format("• Sipariş #%d - %s\n", order.getId(), order.getStatus()));
                sb.append(String.format("  Tarih: %s\n", order.getCreatedAt().toLocalDate()));
                sb.append(String.format("  Toplam: %.2f TL\n\n", order.getTotalPrice()));
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("Failed to get order status for phone: {}", phone, e);
            return "Sipariş durumu alınırken hata oluştu. Lütfen tekrar deneyin.";
        }
    }

    private Long resolveMarketIdByPhone(String phone) {
        try {
            return marketService.findByPhoneNormalized(phone)
                    .map(Market::getId)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Failed to resolve market for phone: {}", phone, e);
            return null;
        }
    }
}