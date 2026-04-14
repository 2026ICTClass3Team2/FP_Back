package com.example.demo.domain.content.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class PostCreateRequestDto {

    @NotBlank(message = "제목을 입력해주세요.")
    private String title;

    @NotBlank(message = "본문을 입력해주세요.")
    private String body;

    // 선택 사항
    private String thumbnailUrl;
    private String contentType; // 기본값은 feed
    private Long channelId; 
    private List<String> tags;
}
