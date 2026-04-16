package com.example.demo.domain.report.entity;

import com.example.demo.domain.report.enums.HiddenReasonType;
import com.example.demo.domain.report.enums.HiddenTargetType;
import com.example.demo.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "hidden",
        indexes = {
                @Index(name = "idx_hidden_target", columnList = "user_id, target_type, target_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_hidden_pair", columnNames = {"user_id", "target_id"})
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Hidden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hidden_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private HiddenTargetType targetType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private HiddenReasonType reason = HiddenReasonType.not_interested;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
