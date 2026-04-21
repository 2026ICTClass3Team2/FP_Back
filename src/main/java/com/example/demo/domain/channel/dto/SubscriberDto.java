package com.example.demo.domain.channel.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubscriberDto {
    private Long userId;
    private String nickname;
    private String username;
    private String profilePicUrl;
}
