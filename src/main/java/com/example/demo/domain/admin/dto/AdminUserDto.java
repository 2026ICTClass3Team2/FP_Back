package com.example.demo.domain.admin.dto;

import com.example.demo.domain.user.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserDto {
    private Long id;
    private String nickname;
    private String username;
    private String email;
    private String status;
    private Integer currentPoint;
    private Integer warningCount;
    private Boolean isSuspended;
    private LocalDateTime registeredAt;

    public AdminUserDto(User user) {
        this.id = user.getId();
        this.nickname = user.getNickname();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.status = user.getStatus().name();
        this.currentPoint = user.getCurrentPoint();
        this.warningCount = user.getWarningCount();
        this.isSuspended = user.getIsSuspended();
        this.registeredAt = user.getRegisteredAt();
    }
}
