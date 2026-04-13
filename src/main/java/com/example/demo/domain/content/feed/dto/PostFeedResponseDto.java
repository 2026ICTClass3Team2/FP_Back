package com.example.demo.domain.content.feed.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostFeedResponseDto {
    // 1. 게시글 정보 (본문 제외)
    private Long postId;
    private String title;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Seoul")
    private LocalDateTime createdAt;
    
    private List<String> tags;

    // 2. 작성자 정보
    private String authorProfileImageUrl;
    private String authorNickname;
    private String authorUsername;

    // 3. 채널 정보 (일반 글은 null)
    private String channelName;

    // 4. 통계 정보
    private int likeCount;
    private int dislikeCount;
    private int viewCount;
    private int commentCount;
    private int shareCount;

    // 5. 사용자 상태 정보
    private boolean isLiked;
    private boolean isDisliked;
    private boolean isBookmarked;
    private boolean isAuthor;
}
