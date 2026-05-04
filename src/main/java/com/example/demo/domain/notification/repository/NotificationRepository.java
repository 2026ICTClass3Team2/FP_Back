package com.example.demo.domain.notification.repository;

import com.example.demo.domain.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    // For general list in MyPage
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Notification> findByUserIdAndIsReadTrueOrderByCreatedAtDesc(Long userId);
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id IN :ids AND n.user.id = :userId")
    void markAsRead(@Param("ids") List<Long> ids, @Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsRead(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.targetType = :targetType AND n.targetId = :targetId AND n.isRead = false")
    void markTargetNotificationsAsRead(@Param("userId") Long userId, @Param("targetType") com.example.demo.domain.notification.entity.NotificationTargetType targetType, @Param("targetId") Long targetId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.targetType = :targetType AND n.isRead = false")
    void markAllTargetTypeNotificationsAsRead(@Param("userId") Long userId, @Param("targetType") com.example.demo.domain.notification.entity.NotificationTargetType targetType);

    long countByUserIdAndIsReadFalse(Long userId);
}
