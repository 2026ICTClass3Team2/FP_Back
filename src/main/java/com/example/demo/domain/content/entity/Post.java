package com.example.demo.domain.content.entity;

import com.example.demo.domain.channel.entity.Channel;
import com.example.demo.domain.user.entity.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "post")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    // 작성자 이름 매핑
    @JsonProperty("author_name")
    @Column(name = "author", length = 100)
    private String authorName;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    // 컨텐츠 타입
    @JsonProperty("content_type")
    @Column(name = "content_type", nullable = false)
    @Builder.Default
    private String contentType = "feed";

    // 소스 타입
    @JsonProperty("source_type")
    @Column(name = "source_type", nullable = false)
    @Builder.Default
    private String sourceType = "internal";

    @Column(nullable = false)
    @Builder.Default
    private String status = "active";

    @Column(name = "external_id", unique = true, length = 100)
    private String externalId;

    @Column(name = "external_url", columnDefinition = "TEXT")
    private String externalUrl;

    @Column(name = "is_hidden", nullable = false)
    @Builder.Default
    private Boolean isHidden = false;

    @Column(name = "is_solved", nullable = false)
    @Builder.Default
    private Boolean isSolved = false;

    // 댓글 수
    @JsonProperty("comment_count")
    @Column(name = "comment_count", nullable = false)
    @Builder.Default
    private Integer commentCount = 0;

    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    @Column(name = "dislike_count", nullable = false)
    @Builder.Default
    private Integer dislikeCount = 0;

    // 조회수
    @JsonProperty("view_count")
    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id")
    private Channel channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User author; // DB 외래키 연관관계용

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ContentTag> contentTags = new ArrayList<>();
}