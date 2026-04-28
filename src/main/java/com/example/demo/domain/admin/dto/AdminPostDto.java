package com.example.demo.domain.admin.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class AdminPostDto {
    private String title;
    private String body;
    private String tag;
    private boolean visible;

    // 파일과 URL을 받기 위한 필드 추가
    private MultipartFile file;
    private String fileUrl;
}