package com.example.demo.domain.interaction.entity;

import com.example.demo.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// @Entity
@Table(
        name = "view_history",
        indexes = {
                @Index(name = "idx_view_history_user_target", columnList = "user_id, target_type, target_id")
        }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ViewHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "view_id")
    private Long id;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType; // 'posts'

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
