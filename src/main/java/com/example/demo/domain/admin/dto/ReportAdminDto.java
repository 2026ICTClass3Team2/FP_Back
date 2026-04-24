package com.example.demo.domain.admin.dto;

import com.example.demo.domain.report.entity.Report;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReportAdminDto {
    private Long id;
    private Long targetId;
    private String targetType;
    private String category;
    private String details;
    private String status;
    private LocalDateTime createdAt;
    private String reporterUsername;

    public ReportAdminDto(Report report) {
        this.id = report.getId();
        this.targetId = report.getTargetId();
        this.targetType = report.getTargetType().name();
        this.category = report.getCategory().name();
        this.details = report.getDetails();
        this.status = report.getStatus().name();
        this.createdAt = report.getCreatedAt();
        if (report.getReporter() != null) {
            this.reporterUsername = report.getReporter().getUsername();
        }
    }
}
