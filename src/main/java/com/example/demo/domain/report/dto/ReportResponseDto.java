package com.example.demo.domain.report.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReportResponseDto {
    private String message;
    private Long reportedId;
    private boolean blocked;
}
