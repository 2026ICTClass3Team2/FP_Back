package com.example.demo.domain.mypage.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class MyPageProfileResponseDto {
    private String profilePicUrl;
    private String nickname;
    private String username;
    private String email;
    private LocalDateTime registeredAt;
    private Integer currentPoint;
    private List<String> techStacks;
}
