package com.example.demo.domain.study.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HiddenLangDetailDto {
    private Long resourceId;
    private String name;
    private String firstChapterContent;
    private String hiddenAt;
}
