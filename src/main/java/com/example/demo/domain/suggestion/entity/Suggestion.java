package com.example.demo.domain.suggestion.entity;

import com.example.demo.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "suggestion")
public class Suggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "suggestion_id")
    private Long id;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "is_seen")
    @Builder.Default
    private Boolean isSeen = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
