package com.example.marketsupplier.service;

import com.example.marketsupplier.entity.Notification;
import com.example.marketsupplier.entity.User;
import com.example.marketsupplier.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class NotificationService {
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    // Bildirim oluştur
    public Notification createNotification(User user, String type, String title, String message, 
                                         String priority, String actionUrl, String relatedEntityType, Long relatedEntityId) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setPriority(priority);
        notification.setActionUrl(actionUrl);
        notification.setRelatedEntityType(relatedEntityType);
        notification.setRelatedEntityId(relatedEntityId);
        notification.setIsRead(false);
        
        return notificationRepository.save(notification);
    }
    
    // Kullanıcının tüm bildirimlerini getir
    public List<Notification> getUserNotifications(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    // Kullanıcının okunmamış bildirimlerini getir
    public List<Notification> getUnreadNotifications(User user) {
        return notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user);
    }
    
    // Kullanıcının okunmamış bildirim sayısını getir
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }
    
    // Kullanıcının son N bildirimini getir
    public List<Notification> getRecentNotifications(User user, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return notificationRepository.findTopNByUserOrderByCreatedAtDesc(user, pageable);
    }
    
    // Bildirimi okundu olarak işaretle
    public Notification markAsRead(Long notificationId, User user) {
        Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
        if (notificationOpt.isPresent()) {
            Notification notification = notificationOpt.get();
            // Kullanıcının kendi bildirimi olduğunu kontrol et
            if (notification.getUser().getId().equals(user.getId())) {
                notification.setIsRead(true);
                return notificationRepository.save(notification);
            }
        }
        return null;
    }
    
    // Kullanıcının tüm bildirimlerini okundu olarak işaretle
    public void markAllAsRead(User user) {
        List<Notification> unreadNotifications = getUnreadNotifications(user);
        for (Notification notification : unreadNotifications) {
            notification.setIsRead(true);
        }
        notificationRepository.saveAll(unreadNotifications);
    }
    
    // Yeni market eklendiğinde bildirim oluştur
    public void notifyNewMarket(User marketUser, String marketName) {
        createNotification(
            marketUser,
            "market",
            "Market Eklendi",
            "Yeni market '" + marketName + "' başarıyla eklendi.",
            "normal",
            "/markets",
            "Market",
            null
        );
    }
    
    // Yeni sipariş geldiğinde bildirim oluştur (tedarikçiye)
    public void notifyNewOrder(User supplierUser, String marketName, Long orderId) {
        createNotification(
            supplierUser,
            "order",
            "Yeni Sipariş!",
            "Bekleyen siparişiniz var. '" + marketName + "' marketinden yeni sipariş geldi.",
            "high",
            "/orders",
            "Order",
            orderId
        );
    }
    
    // Sipariş durumu değiştiğinde bildirim oluştur (market sahibine)
    public void notifyOrderStatusChange(User marketUser, String status, Long orderId) {
        String statusText = getOrderStatusText(status);
        createNotification(
            marketUser,
            "order",
            "Sipariş Durumu Güncellendi",
            "Siparişiniz '" + statusText + "' durumuna güncellendi.",
            "normal",
            "/orders",
            "Order",
            orderId
        );
    }
    
    // Teslimat oluşturulduğunda bildirim oluştur
    public void notifyNewDelivery(User supplierUser, String marketName, Long deliveryId) {
        createNotification(
            supplierUser,
            "delivery",
            "Teslimat Oluşturuldu",
            "Teslimatınız '" + marketName + "' marketine oluşturuldu.",
            "normal",
            "/deliveries",
            "Delivery",
            deliveryId
        );
    }
    
    // Teslimat durumu değiştiğinde bildirim oluştur
    public void notifyDeliveryStatusChange(User user, String status, String marketName, Long deliveryId) {
        String statusText = getDeliveryStatusText(status);
        createNotification(
            user,
            "delivery",
            "Teslimat Durumu Güncellendi",
            "Teslimatınız '" + marketName + "' marketine '" + statusText + "' durumuna güncellendi.",
            status.equals("DELIVERED") ? "normal" : "high",
            "/deliveries",
            "Delivery",
            deliveryId
        );
    }
    
    // Sistem bildirimi oluştur
    public void notifySystemMessage(User user, String title, String message) {
        createNotification(
            user,
            "system",
            title,
            message,
            "normal",
            "/settings",
            null,
            null
        );
    }
    
    private String getOrderStatusText(String status) {
        switch (status) {
            case "PENDING": return "Beklemede";
            case "CONFIRMED": return "Onaylandı";
            case "IN_PROGRESS": return "Hazırlanıyor";
            case "DELIVERED": return "Teslim Edildi";
            case "CANCELLED": return "İptal Edildi";
            default: return status;
        }
    }
    
    private String getDeliveryStatusText(String status) {
        switch (status) {
            case "PENDING": return "Beklemede";
            case "IN_PROGRESS": return "Yolda";
            case "DELIVERED": return "Teslim Edildi";
            case "FAILED": return "Başarısız";
            default: return status;
        }
    }
}
