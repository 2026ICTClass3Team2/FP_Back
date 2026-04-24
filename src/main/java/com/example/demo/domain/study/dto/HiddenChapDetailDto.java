package com.example.demo.domain.study.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HiddenChapDetailDto {
    private Long originalId;
    private String languageName;
    private String title;
    private String firstContent;
    private String hiddenAt;
}
