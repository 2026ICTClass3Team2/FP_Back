package com.example.demo.domain.notification.dto;

import com.example.demo.domain.notification.entity.Notification;
import com.example.demo.domain.notification.entity.NotificationTargetType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationResponseDto {
    private Long id;
    private String message;
    private String targetType;
    private Long targetId;
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
}
