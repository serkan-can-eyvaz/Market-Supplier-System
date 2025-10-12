package com.example.marketsupplier.repository;

import com.example.marketsupplier.entity.Notification;
import com.example.marketsupplier.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    // Kullanıcının tüm bildirimlerini tarih sırasına göre getir
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    
    // Kullanıcının okunmamış bildirimlerini getir
    List<Notification> findByUserAndIsReadFalseOrderByCreatedAtDesc(User user);
    
    // Kullanıcının okunmamış bildirim sayısını getir
    long countByUserAndIsReadFalse(User user);
    
    // Kullanıcının belirli tipteki bildirimlerini getir
    List<Notification> findByUserAndTypeOrderByCreatedAtDesc(User user, String type);
    
    // Kullanıcının yüksek öncelikli bildirimlerini getir
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.priority = 'high' ORDER BY n.createdAt DESC")
    List<Notification> findHighPriorityNotificationsByUser(@Param("user") User user);
    
    // Kullanıcının son N bildirimini getir
    @Query("SELECT n FROM Notification n WHERE n.user = :user ORDER BY n.createdAt DESC")
    List<Notification> findTopNByUserOrderByCreatedAtDesc(@Param("user") User user, org.springframework.data.domain.Pageable pageable);
    
    // Kullanıcının belirli bir tarihten sonraki bildirimlerini getir
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.createdAt > :since ORDER BY n.createdAt DESC")
    List<Notification> findByUserAndCreatedAtAfterOrderByCreatedAtDesc(@Param("user") User user, @Param("since") java.time.LocalDateTime since);
}
