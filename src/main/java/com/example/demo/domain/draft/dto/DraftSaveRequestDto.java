package com.example.demo.domain.draft.dto;

import lombok.Data;

import java.util.List;

@Data
public class DraftSaveRequestDto {
    private String title;
    private String body;
    private String targetType; // feed | qna
    private Long channelId;
    private List<String> tags;
}
