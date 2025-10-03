package com.example.marketsupplier.controller;

import com.example.marketsupplier.service.CartService;
import com.example.marketsupplier.service.MarketService;
import com.example.marketsupplier.service.AuthService;
import com.example.marketsupplier.entity.CartItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "*")
public class CartController {

    @Autowired private CartService cartService;
    @Autowired private AuthService authService;
    @Autowired private MarketService marketService;

    private Long getUserIdFromToken(String token) {
        try { return authService.validateTokenAndGetUser(token.substring(7)).map(u -> u.getId()).orElse(null);} catch(Exception e){return null;}
    }

    @GetMapping
    public ResponseEntity<?> getCart(@RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message","Invalid token"));
        var marketOpt = marketService.findByUserId(userId);
        if (marketOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message","Market not found"));
        List<CartItem> items = cartService.getItems(marketOpt.get().getId());
        return ResponseEntity.ok(items);
    }

    @PostMapping("/replace")
    public ResponseEntity<?> replace(@RequestHeader("Authorization") String token, @RequestBody List<Map<String,Object>> items) {
        Long userId = getUserIdFromToken(token);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message","Invalid token"));
        var marketOpt = marketService.findByUserId(userId);
        if (marketOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message","Market not found"));
        cartService.replaceItems(marketOpt.get().getId(), items);
        return ResponseEntity.ok(Map.of("message","ok"));
    }

    @PostMapping("/append")
    public ResponseEntity<?> append(@RequestHeader("Authorization") String token, @RequestBody List<Map<String,Object>> items) {
        Long userId = getUserIdFromToken(token);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message","Invalid token"));
        var marketOpt = marketService.findByUserId(userId);
        if (marketOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message","Market not found"));
        cartService.appendItems(marketOpt.get().getId(), items);
        return ResponseEntity.ok(Map.of("message","ok"));
    }

    @PostMapping("/clear")
    public ResponseEntity<?> clear(@RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message","Invalid token"));
        var marketOpt = marketService.findByUserId(userId);
        if (marketOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message","Market not found"));
        cartService.clear(marketOpt.get().getId());
        return ResponseEntity.ok(Map.of("message","ok"));
    }
}


