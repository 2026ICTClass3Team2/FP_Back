package com.example.demo.domain.channel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelSummaryDto {
    private Long channelId;
    private String name;
    private String imageUrl;
    private Integer followerCount;
}
