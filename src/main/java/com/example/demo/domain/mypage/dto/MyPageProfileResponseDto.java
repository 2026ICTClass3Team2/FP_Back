package com.example.demo.domain.mypage.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class MyPageProfileResponseDto {
    private Long userId;
    private String profilePicUrl;
    private String nickname;
    private String username;
    private String email;
    private LocalDateTime registeredAt;
    private Integer currentPoint;
    private List<String> techStacks;
    private String provider; // "local" | "google" | "github" | "kakao"
    private Boolean isBlocked;    // 내가 이 유저를 차단했는지 (null = 자기 자신 조회 시)
    private Boolean isMine;       // 내 프로필인지
    private Boolean isFavorited;  // 내가 이 유저를 즐겨찾기했는지 (null = 자기 자신 조회 시)
}
