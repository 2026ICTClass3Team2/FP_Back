package com.example.demo.domain.content.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PostDetailResponseDto {
    private Long postId;
    private String title;
    private String body;
    private String thumbnailUrl;
    private String contentType;
    
    private Long authorUserId;
    private String authorNickname;
    private String authorProfileImageUrl;
    private String authorUsername;
    private Long channelId;
    private String channelName;
    private String channelImageUrl;
    
    private int likeCount;
    private int dislikeCount;
    private int viewCount;
    private int commentCount;
    
    @JsonProperty("isLiked")
    private boolean isLiked;
    @JsonProperty("isDisliked")
    private boolean isDisliked;
    @JsonProperty("isBookmarked")
    private boolean isBookmarked;
    @JsonProperty("isAuthor")
    private boolean isAuthor;

    private List<String> tags;
    private List<String> attachedUrls;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
