package com.example.demo.domain.report.controller;

import com.example.demo.domain.report.dto.ReportRequestDto;
import com.example.demo.domain.report.dto.ReportResponseDto;
import com.example.demo.domain.report.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<ReportResponseDto> submitReport(
            @Valid @RequestBody ReportRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            // Spring Security 설정에 따라 보통 이 부분은 실행되지 않지만, 안전을 위해 추가
            return ResponseEntity.status(401).build();
        }
        String reporterEmail = userDetails.getUsername();

        boolean isBlocked = reportService.processReport(requestDto, reporterEmail);

        ReportResponseDto response = new ReportResponseDto(
                "신고가 성공적으로 접수되었습니다.",
                requestDto.getTargetId(),
                isBlocked
        );

        return ResponseEntity.ok(response);
    }
}
