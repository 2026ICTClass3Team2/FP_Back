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
    private String username; // Changed from author
    private String nickname; // Added
    private boolean resolved;
    private int points;
    private List<String> techStacks;
    private LocalDateTime createdAt;
    private int commentsCount;
    private int likes;
    private int dislikes;
    private int shares;
    private int views;

    @QueryProjection
    public QnaCardResponseDto(Long qnaId, String title, String body, String username, String nickname, boolean resolved, int points, LocalDateTime createdAt, int commentsCount, int likes, int dislikes, int views) {
        this.qnaId = qnaId;
        this.title = title;
        this.body = body;
        this.username = username;
        this.nickname = nickname;
        this.resolved = resolved;
        this.points = points;
        this.createdAt = createdAt;
        this.commentsCount = commentsCount;
        this.likes = likes;
        this.dislikes = dislikes;
        this.views = views;
    }
}
