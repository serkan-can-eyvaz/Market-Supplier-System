package com.example.marketsupplier.agent.actions;

import com.example.marketsupplier.entity.Cart;
import com.example.marketsupplier.entity.CartItem;
import com.example.marketsupplier.entity.Market;
import com.example.marketsupplier.entity.Product;
import com.example.marketsupplier.repository.CartItemRepository;
import com.example.marketsupplier.repository.CartRepository;
import com.example.marketsupplier.repository.ProductRepository;
import com.example.marketsupplier.service.MarketService;
import com.example.marketsupplier.service.CartService;
import com.example.marketsupplier.agent.context.ContextManager.CartContext;
import com.example.marketsupplier.service.CustomerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
public class CartActionHandler implements ActionHandler {

    private static final Logger log = LoggerFactory.getLogger(CartActionHandler.class);

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MarketService marketService;

    @Autowired
    private CartService cartService;

    @Override
    public String getActionName() { return "cart"; }

    @Override
    public boolean canHandle(String action) {
        return action != null && (action.equals("cart.add") || action.equals("cart.show"));
    }

    public String handle(String phone, String text, CustomerContext context) {
        // Bu handler artık metin-pars etmez; yalnızca mevcut state’i gösterir.
        // Niyet çıkarımı ve state değişikliği ActionExecutor/AgentOrchestrator üzerinden yürütülür.
        return showCart(phone, null);
    }

    @Transactional
    public String addToCart(String phone, String message, CartContext context) {
        try {
            Long marketId = resolveMarketIdByPhone(phone);
            if (marketId == null) return "Numaranız sisteme kayıtlı değil. Lütfen önce kaydınızı tamamlayın.";

            // Cart'ı bul veya oluştur
            Cart cart = getOrCreateCart(marketId);
            if (context != null) {
                context.setCartId(cart.getId());
                context.setMarketId(marketId);
            }

            // Basit ürün eşleştirme (gerçek implementasyon ActionExecutor'da)
            List<Product> products = productRepository.findAll();
            if (products.isEmpty()) {
                return "Şu anda aktif ürün bulunmuyor.";
            }

            // Basit eşleştirme - ilk ürünü al
            Product product = products.get(0);
            int quantity = extractQuantity(message);
            String unit = extractUnit(message);

            // CartItem oluştur
            CartItem cartItem = new CartItem(cart, product.getId(), product.getName(), 
                                           quantity, unit != null ? unit : product.getUnit(), product.getPrice());
            cartItemRepository.save(cartItem);

            log.info("Added item to cart: cartId={}, product={}, quantity={}", cart.getId(), product.getName(), quantity);
            return String.format("✅ %s x%d %s sepete eklendi. Toplam: %.2f TL", 
                               product.getName(), quantity, cartItem.getUnit(), 
                               product.getPrice().multiply(BigDecimal.valueOf(quantity)));

        } catch (Exception e) {
            log.error("Failed to add item to cart for phone: {}", phone, e);
            return "Sepete ürün eklenirken hata oluştu. Lütfen tekrar deneyin.";
        }
    }

    public String showCart(String phone, CartContext context) {
        try {
            Long marketId = resolveMarketIdByPhone(phone);
            if (marketId == null) return "Numaranız sisteme kayıtlı değil. Lütfen önce kaydınızı tamamlayın.";

            List<CartItem> items = cartItemRepository.findByMarketId(marketId);
            if (items.isEmpty()) return "Sepetiniz boş.";

            StringBuilder sb = new StringBuilder("🛒 Sepetiniz:\n");
            BigDecimal total = BigDecimal.ZERO;
            for (CartItem item : items) {
                BigDecimal itemTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                total = total.add(itemTotal);
                sb.append(String.format("• %s x%d %s - %.2f TL\n", 
                    item.getProductName(), item.getQuantity(), item.getUnit(), itemTotal));
            }
            sb.append(String.format("\n💰 Toplam: %.2f TL\n", total));
            sb.append("Onaylamak için 'onayla' yazabilirsiniz.");

            return sb.toString();

        } catch (Exception e) {
            log.error("Failed to show cart for phone: {}", phone, e);
            return "Sepet bilgileri alınırken hata oluştu. Lütfen tekrar deneyin.";
        }
    }

    private Cart getOrCreateCart(Long marketId) {
        Optional<Cart> existingCart = cartRepository.findLatestByMarketId(marketId);
        if (existingCart.isPresent()) {
            return existingCart.get();
        }

        // Yeni cart oluştur
        Optional<Market> marketOpt = marketService.findById(marketId);
        if (marketOpt.isEmpty()) {
            throw new IllegalStateException("Market not found: " + marketId);
        }

        Cart newCart = new Cart(marketOpt.get());
        return cartRepository.save(newCart);
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

    private int extractQuantity(String message) {
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(message);
            if (m.find()) return Integer.parseInt(m.group(1));
        } catch (Exception ignored) {}
        return 1;
    }

    private String extractUnit(String message) {
        String lower = message.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("kilo")) return "kilo";
        if (lower.contains("adet")) return "adet";
        if (lower.contains("kg")) return "kg";
        if (lower.contains("koli")) return "koli";
        return null;
    }

    // Bu sınıfta metin normalizasyonu/parsing yapılmaz.
}