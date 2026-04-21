package com.example.demo.domain.channel.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChannelSummaryDto {
    private Long channelId;
    private String name;
    private String imageUrl;
    private Integer followerCount;
}
