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

    private String thumbnailUrl;
    private String contentType;
    private Long channelId; 
    private List<String> tags;
    private String attachedUrls;
}
