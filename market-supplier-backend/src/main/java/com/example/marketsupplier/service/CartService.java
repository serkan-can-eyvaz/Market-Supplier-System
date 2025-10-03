package com.example.marketsupplier.service;

import com.example.marketsupplier.entity.*;
import com.example.marketsupplier.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@Transactional
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    @Autowired private CartRepository cartRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private MarketService marketService;
    // ProductService circular dependency'yi önlemek için kaldırıldı

    public Cart getOrCreateCartForMarket(Long marketId) {
        Market market = marketService.findById(marketId).orElseThrow(() -> new RuntimeException("Market not found"));
        return cartRepository.findByMarket(market).orElseGet(() -> cartRepository.save(new Cart(market)));
    }
    
    public Cart getOrCreateCartForMarket(Market market) {
        return cartRepository.findByMarket(market).orElseGet(() -> cartRepository.save(new Cart(market)));
    }

    public void addItemToCart(Market market, Product product, int quantity, String unit) {
        Cart cart = getOrCreateCartForMarket(market);
        
        Optional<CartItem> existingItem = cartItemRepository.findByCartAndProductId(cart, product.getId());
        
        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            cartItemRepository.save(item);
        } else {
            CartItem newItem = new CartItem(
                cart, 
                product.getId(), 
                product.getName(), 
                quantity, 
                unit != null ? unit : product.getUnit(), 
                product.getPrice()
            );
            cartItemRepository.save(newItem);
        }
    }

    public List<CartItem> getItems(Long marketId) {
        Cart cart = getOrCreateCartForMarket(marketId);
        return cartItemRepository.findByCart(cart);
    }

    @Transactional
    public void clear(Long marketId) {
        Cart cart = getOrCreateCartForMarket(marketId);
        
        // Önce tüm cart item'ları al
        List<CartItem> items = cartItemRepository.findByCart(cart);
        
        if (!items.isEmpty()) {
            // Bulk delete ile daha agresif silme
            cartItemRepository.deleteAll(items);
            
            // Immediate flush ve commit
            cartItemRepository.flush();
            
            // Doğrula - gerçekten silindi mi?
            List<CartItem> remainingItems = cartItemRepository.findByCart(cart);
            if (!remainingItems.isEmpty()) {
                // Fallback: Manuel tek tek silme
                for (CartItem item : remainingItems) {
                    cartItemRepository.delete(item);
                }
                cartItemRepository.flush();
                
                // Son kontrol
                List<CartItem> finalCheck = cartItemRepository.findByCart(cart);
                if (!finalCheck.isEmpty()) {
                    throw new RuntimeException("Cart items could not be cleared completely after fallback!");
                }
            }
        }
    }

    public void replaceItems(Long marketId, List<Map<String, Object>> items) {
        Cart cart = getOrCreateCartForMarket(marketId);
        cartItemRepository.deleteByCart(cart);
        appendItems(marketId, items);
    }

    public void appendItems(Long marketId, List<Map<String, Object>> items) {
        Cart cart = getOrCreateCartForMarket(marketId);
        Map<Long, CartItem> merged = new HashMap<>();
        for (CartItem ci : cartItemRepository.findByCart(cart)) {
            if (ci.getProductId() != null) merged.put(ci.getProductId(), ci);
        }
        for (Map<String, Object> it : items) {
            Long pid = it.get("product_id") != null ? Long.parseLong(it.get("product_id").toString()) : null;
            String pname = (String) it.get("product_name");
            int qty = Integer.parseInt(it.get("quantity").toString());
            String unit = (String) it.getOrDefault("unit", "adet");
            BigDecimal price = new BigDecimal(it.get("price").toString());
            if (pid != null && merged.containsKey(pid)) {
                CartItem ex = merged.get(pid);
                ex.setQuantity(ex.getQuantity() + qty);
                cartItemRepository.save(ex);
            } else {
                cartItemRepository.save(new CartItem(cart, pid, pname, qty, unit, price));
            }
        }
    }

    public void removeItem(Long marketId, Long itemId) {
        Cart cart = getOrCreateCartForMarket(marketId);
        cartItemRepository.findById(itemId)
            .filter(item -> item.getCart().equals(cart))
            .ifPresent(cartItemRepository::delete);
    }

    public void removeItemByName(Long marketId, String productName) {
        Cart cart = getOrCreateCartForMarket(marketId);
        List<CartItem> items = cartItemRepository.findByCart(cart);

        if (items.isEmpty()) {
            return;
        }

        String query = productName != null ? normalize(productName) : "";

        // 1) Ürün adı boş ve sepette tek ürün varsa otomatik sil
        if ((query.isBlank()) && items.size() == 1) {
            cartItemRepository.delete(items.get(0));
            return;
        }

        // 2) Önce case-insensitive tam eşleşme
        if (!query.isBlank()) {
            cartItemRepository.deleteByCartIdAndName(cart.getId(), productName);
            // deleteByCartIdAndName eşleşmediyse aşağıdaki bulanık eşleşmeye geçer
        }

        // 3) Bulanık eşleşme: en yakın ada göre sil
        if (!query.isBlank()) {
            CartItem best = null;
            double bestScore = 0.0;
            for (CartItem ci : items) {
                double s = similarity(query, normalize(ci.getProductName()));
                if (s > bestScore) { bestScore = s; best = ci; }
            }
            if (best != null && bestScore >= 0.6) {
                cartItemRepository.delete(best);
            }
        }
    }

    public void updateItemQuantity(Long marketId, Long itemId, int newQuantity) {
        Cart cart = getOrCreateCartForMarket(marketId);
        cartItemRepository.findById(itemId)
            .filter(item -> item.getCart().equals(cart))
            .ifPresent(item -> {
                item.setQuantity(newQuantity);
                cartItemRepository.save(item);
            });
    }

    @Transactional
    public void clearCart(Long marketId) {
        Cart cart = getOrCreateCartForMarket(marketId);
        
        // Log before operation
        List<CartItem> beforeItems = cartItemRepository.findByCart(cart);
        log.info("BEFORE CLEAR: {} items in cart for market {}", beforeItems.size(), marketId);
        
        if (!beforeItems.isEmpty()) {
            // Use native SQL for guaranteed deletion
            cartItemRepository.deleteByCartIdNative(cart.getId());
            
            // Force immediate commit
            cartItemRepository.flush();
            
            // Double verification with fresh query
            List<CartItem> afterItems = cartItemRepository.findByCart(cart);
            log.info("AFTER CLEAR: {} items remaining for market {}", afterItems.size(), marketId);
            
            if (!afterItems.isEmpty()) {
                log.error("FALLBACK: Native delete failed, using JPA delete");
                cartItemRepository.deleteAll(afterItems);
                cartItemRepository.flush();
                
                // Final check
                List<CartItem> finalItems = cartItemRepository.findByCart(cart);
                log.info("FINAL CHECK: {} items after fallback for market {}", finalItems.size(), marketId);
            }
        }
    }

    private String normalize(String s) {
        String x = s == null ? "" : s.trim().toLowerCase(java.util.Locale.ROOT);
        // Türkçe karakterleri normalize et
        x = x.replace('ş','s').replace('ı','i').replace('ç','c').replace('ğ','g').replace('ö','o').replace('ü','u');
        return x.replaceAll("\\s+", " ");
    }

    private double similarity(String a, String b) {
        if (a.equals(b)) return 1.0;
        // basit trigram kesişimi + kelime örtüşmesi ortalaması
        double tri = trigram(a, b);
        double word = wordOverlap(a, b);
        return 0.6 * tri + 0.4 * word;
    }

    private double trigram(String a, String b) {
        java.util.Set<String> ta = trigrams(a);
        java.util.Set<String> tb = trigrams(b);
        if (ta.isEmpty() && tb.isEmpty()) return 1.0;
        int inter = 0;
        for (String t : ta) if (tb.contains(t)) inter++;
        int union = ta.size() + tb.size() - inter;
        return union == 0 ? 0.0 : (double) inter / union;
    }

    private java.util.Set<String> trigrams(String s) {
        java.util.Set<String> set = new java.util.HashSet<>();
        if (s == null) return set;
        String x = s.replaceAll("\\s+", " ");
        if (x.length() < 3) { if (!x.isEmpty()) set.add(x); return set; }
        for (int i = 0; i < x.length() - 2; i++) set.add(x.substring(i, i + 3));
        return set;
    }

    private double wordOverlap(String a, String b) {
        java.util.Set<String> wa = new java.util.HashSet<>(java.util.Arrays.asList(a.split("\\s+")));
        java.util.Set<String> wb = new java.util.HashSet<>(java.util.Arrays.asList(b.split("\\s+")));
        if (wa.isEmpty() && wb.isEmpty()) return 1.0;
        int inter = 0;
        for (String w : wa) if (wb.contains(w)) inter++;
        int union = wa.size() + wb.size() - inter;
        return union == 0 ? 0.0 : (double) inter / union;
    }
}


