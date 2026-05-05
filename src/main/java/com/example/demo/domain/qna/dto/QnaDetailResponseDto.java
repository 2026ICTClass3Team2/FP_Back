package com.example.demo.domain.qna.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class QnaDetailResponseDto {
    private Long qnaId;
    private Long postId;
    private String title;
    private String body;
    private String username;
    private String nickname;
    private String authorProfileImageUrl;
    private Long userId;
    private boolean resolved;
    private int points;         // 총 보상 포인트 (manual + LLM)
    private int manualRewardPoints; // 작성자 수동 설정 포인트
    private int llmScore;           // LLM 난이도 점수
    private List<String> techStacks;
    private LocalDateTime createdAt;
    private int commentCount;
    private int likeCount;
    private int dislikeCount;
    private int viewCount;

    private boolean isEvent;
    private int eventPoints;

    private boolean isLiked;
    private boolean isDisliked;
    private boolean isBookmarked;
    private boolean isAuthor;
}
