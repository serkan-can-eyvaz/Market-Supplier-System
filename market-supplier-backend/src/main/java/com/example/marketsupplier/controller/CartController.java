package com.example.marketsupplier.controller;

import com.example.marketsupplier.service.CartService;
import com.example.marketsupplier.service.MarketService;
import com.example.marketsupplier.service.OrderPdfService;
import com.example.marketsupplier.entity.CartItem;
import com.example.marketsupplier.entity.User;
import com.example.marketsupplier.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "*")
public class CartController {

    @Autowired private CartService cartService;
    @Autowired private MarketService marketService;
    @Autowired private OrderPdfService orderPdfService;

    @GetMapping
    public ResponseEntity<?> getCart(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        var marketOpt = marketService.findByUserId(user.getId());
        if (marketOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message","Market not found"));
        List<CartItem> items = cartService.getItems(marketOpt.get().getId());
        return ResponseEntity.ok(items);
    }

    @PostMapping("/replace")
    public ResponseEntity<?> replace(Authentication authentication, @RequestBody List<Map<String,Object>> items) {
        User user = (User) authentication.getPrincipal();
        var marketOpt = marketService.findByUserId(user.getId());
        if (marketOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message","Market not found"));
        cartService.replaceItems(marketOpt.get().getId(), items);
        return ResponseEntity.ok(Map.of("message","ok"));
    }

    @PostMapping("/append")
    public ResponseEntity<?> append(Authentication authentication, @RequestBody List<Map<String,Object>> items) {
        User user = (User) authentication.getPrincipal();
        var marketOpt = marketService.findByUserId(user.getId());
        if (marketOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message","Market not found"));
        cartService.appendItems(marketOpt.get().getId(), items);
        return ResponseEntity.ok(Map.of("message","ok"));
    }

    @PostMapping("/clear")
    public ResponseEntity<?> clear(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        var marketOpt = marketService.findByUserId(user.getId());
        if (marketOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message","Market not found"));
        cartService.clear(marketOpt.get().getId());
        return ResponseEntity.ok(Map.of("message","ok"));
    }

    // Yeni gelişmiş sepet endpoint'leri
    @PostMapping("/add-item")
    public ResponseEntity<?> addItem(Authentication authentication, 
                                    @Valid @RequestBody CartItemRequest request) {
        try {
            User user = (User) authentication.getPrincipal();
            var marketOpt = marketService.findByUserId(user.getId());
            if (marketOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message","Market not found"));
            
            cartService.addItem(marketOpt.get().getId(), request.getProductId(), request.getQuantity());
            
            // Güncellenmiş sepeti döndür
            CartResponse cartResponse = cartService.getCartResponse(marketOpt.get().getId());
            return ResponseEntity.ok(cartResponse);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/update-item/{itemId}")
    public ResponseEntity<?> updateItem(Authentication authentication,
                                       @PathVariable Long itemId,
                                       @RequestParam Integer quantity) {
        try {
            User user = (User) authentication.getPrincipal();
            var marketOpt = marketService.findByUserId(user.getId());
            if (marketOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message","Market not found"));
            
            cartService.updateItemQuantity(marketOpt.get().getId(), itemId, quantity);
            
            // Güncellenmiş sepeti döndür
            CartResponse cartResponse = cartService.getCartResponse(marketOpt.get().getId());
            return ResponseEntity.ok(cartResponse);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/remove-item/{itemId}")
    public ResponseEntity<?> removeItem(Authentication authentication,
                                       @PathVariable Long itemId) {
        try {
            User user = (User) authentication.getPrincipal();
            var marketOpt = marketService.findByUserId(user.getId());
            if (marketOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message","Market not found"));
            
            cartService.removeItem(marketOpt.get().getId(), itemId);
            
            // Güncellenmiş sepeti döndür
            CartResponse cartResponse = cartService.getCartResponse(marketOpt.get().getId());
            return ResponseEntity.ok(cartResponse);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/detailed")
    public ResponseEntity<?> getCartDetailed(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            var marketOpt = marketService.findByUserId(user.getId());
            if (marketOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message","Market not found"));
            
            CartResponse cartResponse = cartService.getCartResponse(marketOpt.get().getId());
            return ResponseEntity.ok(cartResponse);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getCartSummary(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            var marketOpt = marketService.findByUserId(user.getId());
            if (marketOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message","Market not found"));
            
            Map<String, Object> summary = cartService.getCartSummary(marketOpt.get().getId());
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/pdf")
    public ResponseEntity<?> downloadCartPdf(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            var marketOpt = marketService.findByUserId(user.getId());
            if (marketOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message","Market not found"));
            
            CartResponse cartResponse = cartService.getCartResponse(marketOpt.get().getId());
            
            if (cartResponse.getItems().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Sepet boş, PDF oluşturulamaz"));
            }
            
            byte[] pdfBytes = orderPdfService.generateCartPdf(cartResponse);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "sepet-detayi-" + cartResponse.getId() + ".pdf");
            headers.setContentLength(pdfBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
                    
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "PDF oluşturulurken hata: " + e.getMessage()));
        }
    }
}


