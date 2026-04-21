package com.example.demo.domain.admin.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SuspendRequestDto {
    private String reason;
    private LocalDateTime releasedAt;
}
