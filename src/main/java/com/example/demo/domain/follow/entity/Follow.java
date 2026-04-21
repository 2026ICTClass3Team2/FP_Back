package com.example.demo.domain.follow.entity;

import com.example.demo.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "follow",
        uniqueConstraints = @UniqueConstraint(name = "uk_follow_pair", columnNames = {"user_id", "target_id", "target_type"}),
        indexes = @Index(name = "idx_follow_id", columnList = "target_id, target_type, is_muted, followed_at DESC")
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "follow_id")
    private Long id;

    @Column(name = "target_type", nullable = false, length = 10)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "is_muted", nullable = false)
    @Builder.Default
    private Boolean isMuted = false;

    @CreationTimestamp
    @Column(name = "followed_at", nullable = false, updatable = false)
    private LocalDateTime followedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
