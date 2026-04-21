package com.example.demo.domain.channel.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChannelDetailDto {
    private Long channelId;
    private String name;
    private String description;
    private String imageUrl;
    private Integer followerCount;
    private Integer postCount;
    private String status;
    private List<String> techStacks;
    private boolean subscribed;
    private String ownerNickname;
    private String ownerUsername;
    private Long ownerId;
}
