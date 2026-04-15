package com.example.demo.domain.interaction.entity;

import com.example.demo.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "interaction",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_interaction_user_target", columnNames = {"user_id", "target_type", "target_id"})
        }
)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Interaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interaction_id")
    private Long id;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType; // 'qna', 'feed', 'comments'

    @Column(name = "action_type", nullable = false, length = 20)
    private String actionType; // 'like', 'dislike'

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
