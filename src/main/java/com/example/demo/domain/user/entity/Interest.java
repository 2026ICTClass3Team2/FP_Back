package com.example.demo.domain.user.entity;

import com.example.demo.domain.content.entity.Tag;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "interest")
public class Interest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interest_id")
    private Long id;

    @Builder.Default
    @Column(name = "weight_score", nullable = false)
    private Double weightScore = 1.0;

    @Builder.Default
    @Column(name = "is_profile_tag", nullable = false)
    private Boolean isProfileTag = false;

    @Column(name = "last_interaction_at", nullable = false)
    private LocalDateTime lastInteractionAt;

    public void updateScore(double newScore, LocalDateTime now) {
        this.weightScore = newScore;
        this.lastInteractionAt = now;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;
}
