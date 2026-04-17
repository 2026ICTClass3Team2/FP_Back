package com.example.demo.domain.content.dto;

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
    
    private String authorNickname;
    private String authorProfileImageUrl;
    private String authorUsername;
    private String channelName;
    
    private int likeCount;
    private int dislikeCount;
    private int viewCount;
    private int commentCount;
    
    private boolean isLiked;
    private boolean isDisliked;
    private boolean isBookmarked;
    private boolean isAuthor;

    private List<String> tags;
    private List<String> attachedUrls;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
