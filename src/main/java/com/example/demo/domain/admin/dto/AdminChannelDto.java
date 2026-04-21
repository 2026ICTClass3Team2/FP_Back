package com.example.demo.domain.admin.dto;

import com.example.demo.domain.channel.entity.Channel;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class AdminChannelDto {
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private Integer followerCount;
    private Integer postCount;
    private String status;
    private LocalDateTime createdAt;
    private String ownerNickname;
    private String ownerUsername;
    private List<String> techStacks;

    public AdminChannelDto(Channel channel, List<String> techStacks) {
        this.id = channel.getId();
        this.name = channel.getName();
        this.description = channel.getDescription();
        this.imageUrl = channel.getImageUrl();
        this.followerCount = channel.getFollowerCount();
        this.postCount = channel.getPostCount();
        this.status = channel.getStatus();
        this.createdAt = channel.getCreatedAt();
        
        if (channel.getOwner() != null) {
            this.ownerNickname = channel.getOwner().getNickname();
            this.ownerUsername = channel.getOwner().getUsername();
        }
        
        this.techStacks = techStacks;
    }
}
