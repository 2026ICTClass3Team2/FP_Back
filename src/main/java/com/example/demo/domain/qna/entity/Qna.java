package com.example.demo.domain.qna.entity;

import com.example.demo.domain.content.entity.Post;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "qna")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Qna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "qna_id")
    private Long id;

    @Column(name = "is_solved", nullable = false)
    private boolean isSolved;

    @Column(name = "reward_points", nullable = false)
    private int rewardPoints;

    @Column(name = "answer_id")
    private Long answerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
}