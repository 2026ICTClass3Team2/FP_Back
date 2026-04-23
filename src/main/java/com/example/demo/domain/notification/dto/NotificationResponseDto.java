package com.example.demo.domain.notification.dto;

import com.example.demo.domain.notification.entity.Notification;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationResponseDto {
    private Long id;
    private String message;
    private String targetType;
    private Long targetId;
    private Long postId; // For comments/mentions
    private Long qnaId;
    private boolean isRead;
    private LocalDateTime createdAt;

    public NotificationResponseDto(Notification notification) {
        this.id = notification.getId();
        this.message = notification.getNotificationType();
        this.targetType = notification.getTargetType().name();
        this.targetId = notification.getTargetId();
        this.isRead = notification.isRead();
        this.createdAt = notification.getCreatedAt();
    }

    public NotificationResponseDto(Notification notification, Long postId) {
        this(notification);
        this.postId = postId;
    }
}
