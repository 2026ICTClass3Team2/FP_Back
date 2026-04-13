package com.example.demo.domain.content.feed.entity;

import com.example.demo.domain.content.entity.Post;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "content_tag")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContentTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "content_tag_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;
}
