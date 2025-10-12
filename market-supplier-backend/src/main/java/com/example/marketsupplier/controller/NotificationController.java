package com.example.marketsupplier.controller;

import com.example.marketsupplier.entity.Notification;
import com.example.marketsupplier.entity.User;
import com.example.marketsupplier.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {
    
    @Autowired
    private NotificationService notificationService;
    
    // Kullanıcının bildirimlerini getir
    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications(Authentication authentication) {
        try {
            User user = getUserFromAuthentication(authentication);
            List<Notification> notifications = notificationService.getUserNotifications(user);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Kullanıcının okunmamış bildirimlerini getir
    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(Authentication authentication) {
        try {
            User user = getUserFromAuthentication(authentication);
            List<Notification> notifications = notificationService.getUnreadNotifications(user);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Kullanıcının okunmamış bildirim sayısını getir
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(Authentication authentication) {
        try {
            User user = getUserFromAuthentication(authentication);
            long count = notificationService.getUnreadCount(user);
            Map<String, Object> response = new HashMap<>();
            response.put("count", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Kullanıcının son N bildirimini getir
    @GetMapping("/recent")
    public ResponseEntity<List<Notification>> getRecentNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            User user = getUserFromAuthentication(authentication);
            List<Notification> notifications = notificationService.getRecentNotifications(user, limit);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Bildirimi okundu olarak işaretle
    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable Long id, Authentication authentication) {
        try {
            User user = getUserFromAuthentication(authentication);
            Notification notification = notificationService.markAsRead(id, user);
            if (notification != null) {
                return ResponseEntity.ok(notification);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Kullanıcının tüm bildirimlerini okundu olarak işaretle
    @PutMapping("/mark-all-read")
    public ResponseEntity<Map<String, String>> markAllAsRead(Authentication authentication) {
        try {
            User user = getUserFromAuthentication(authentication);
            notificationService.markAllAsRead(user);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Tüm bildirimler okundu olarak işaretlendi");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Son kontrol tarihinden sonraki bildirimleri getir (polling için)
    @GetMapping("/since")
    public ResponseEntity<List<Notification>> getNotificationsSince(
            Authentication authentication,
            @RequestParam String since) {
        try {
            User user = getUserFromAuthentication(authentication);
            // Bu endpoint için NotificationRepository'ye yeni method eklenebilir
            List<Notification> notifications = notificationService.getUserNotifications(user);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Authentication'dan User objesini al (ProductController'dan kopyalandı)
    private User getUserFromAuthentication(Authentication authentication) {
        if (authentication.getPrincipal() instanceof com.example.marketsupplier.config.CustomUserDetailsService.CustomUserPrincipal) {
            com.example.marketsupplier.config.CustomUserDetailsService.CustomUserPrincipal principal = 
                (com.example.marketsupplier.config.CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
            return principal.getUser();
        } else if (authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        } else {
            throw new RuntimeException("Authentication principal type not supported");
        }
    }
}
