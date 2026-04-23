package com.example.demo.domain.report.dto;

import com.example.demo.domain.report.enums.ReportReasonType;
import com.example.demo.domain.report.enums.ReportTargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ReportRequestDto {

    @NotNull
    private ReportTargetType targetType;

    @NotNull
    private Long targetId;

    @NotNull
    private ReportReasonType reasonType;

    @NotNull
    @Size(min = 10, max = 500)
    private String reasonDetail;

    private boolean blockPost = false;

    private boolean blockUser = false;

    private boolean blockChannel = false;
}
