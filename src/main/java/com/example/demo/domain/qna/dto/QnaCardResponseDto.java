package com.example.demo.domain.qna.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class QnaCardResponseDto {
    private Long qnaId;
    private String title;
    private String body;
    private String username;
    private String nickname;
    private String authorProfileImageUrl;
    private boolean resolved;
    private int points;
    private List<String> techStacks;
    private LocalDateTime createdAt;
    private int commentCount;
    private int likeCount;
    private int dislikeCount;
    private int shareCount;
    private int viewCount;

    private boolean isLiked;
    private boolean isDisliked;
    private boolean isBookmarked;
    private boolean isAuthor;

    @QueryProjection
    public QnaCardResponseDto(Long qnaId, String title, String body, String username, String nickname, String authorProfileImageUrl, boolean resolved, int points, LocalDateTime createdAt, int commentCount, int likeCount, int dislikeCount, int viewCount) {
        this.qnaId = qnaId;
        this.title = title;
        this.body = body;
        this.username = username;
        this.nickname = nickname;
        this.authorProfileImageUrl = authorProfileImageUrl;
        this.resolved = resolved;
        this.points = points;
        this.createdAt = createdAt;
        this.commentCount = commentCount;
        this.likeCount = likeCount;
        this.dislikeCount = dislikeCount;
        this.viewCount = viewCount;
        this.shareCount = 0; // Defaulting share count if not in projection
    }
}
