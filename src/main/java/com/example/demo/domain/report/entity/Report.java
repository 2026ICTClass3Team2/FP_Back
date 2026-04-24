package com.example.demo.domain.report.entity;

import com.example.demo.domain.report.enums.ReportReasonType;
import com.example.demo.domain.report.enums.ReportStatus;
import com.example.demo.domain.report.enums.ReportTargetType;
import com.example.demo.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "report",
        indexes = {
                @Index(name = "idx_report_status", columnList = "status, created_at"),
                @Index(name = "idx_report_type", columnList = "target_type, target_id")
        }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private ReportTargetType targetType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReasonType category;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private User reporter;
}
