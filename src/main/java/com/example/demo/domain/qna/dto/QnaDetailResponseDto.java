package com.example.demo.domain.qna.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class QnaDetailResponseDto {
    private Long qnaId;
    private String title;
    private String body;
    private String author;
    private boolean resolved;
    private int points;
    private String imageUrl;
    private List<String> techStacks;
    private LocalDateTime createdAt;
    private int commentsCount;
    private int likes;
    private int dislikes;
    private int shares;
    private int views;
    private String category; // Assuming category might be added later or derived
}
