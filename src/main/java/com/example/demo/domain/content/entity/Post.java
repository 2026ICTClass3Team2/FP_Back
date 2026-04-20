package com.example.demo.domain.content.entity;

import com.example.demo.domain.channel.entity.Channel;
import com.example.demo.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "post",
        indexes = {
                // @Index(name = "idx_post_search", columnList = "author, title, body"), // FULLTEXT INDEX는 JPA에서 직접 지원하지 않아 DDL로 처리하거나 하이버네이트 애노테이션 필요
                @Index(name = "idx_post_created", columnList = "created_at DESC"),
                @Index(name = "idx_post_channel", columnList = "channel_id"),
                @Index(name = "idx_post_content", columnList = "content_type, created_at DESC"),
                @Index(name = "idx_post_solved", columnList = "is_solved, created_at DESC"),
                @Index(name = "idx_post_like", columnList = "like_count DESC, created_at DESC"),
                @Index(name = "idx_post_dislike", columnList = "dislike_count DESC, created_at DESC"),
                @Index(name = "idx_post_view", columnList = "view_count DESC, created_at DESC")
        }
)
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

    @Column(name = "author", length = 100)
    private String authorName; // 외부 API 등에서 가져온 작성자 이름 (컬럼명 author에 매핑)

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    @Column(name = "content_type", nullable = false)
    @Builder.Default
    private String contentType = "feed"; // feed, qna

    @Column(name = "source_type", nullable = false)
    @Builder.Default
    private String sourceType = "internal"; // internal, external

    @Column(nullable = false)
    @Builder.Default
    private String status = "active"; // active, hidden, frozen

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

    @Column(name = "comment_count", nullable = false)
    @Builder.Default
    private Integer commentCount = 0;

    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    @Column(name = "dislike_count", nullable = false)
    @Builder.Default
    private Integer dislikeCount = 0;

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
    private User author;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ContentTag> contentTags = new ArrayList<>();
}