package com.example.demo.domain.qna.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class QnaDetailResponseDto {
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
}
