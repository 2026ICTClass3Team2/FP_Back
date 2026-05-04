package com.example.demo.domain.content.entity;

import com.example.demo.domain.draft.entity.Draft;
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
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "draft_id")
    private Draft draft;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    public String getTagName() {
        return tag != null ? tag.getName() : null;
    }
}

