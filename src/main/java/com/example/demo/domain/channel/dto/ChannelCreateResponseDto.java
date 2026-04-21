package com.example.demo.domain.channel.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChannelCreateResponseDto {
    private Long channelId;
    private String message;
}
