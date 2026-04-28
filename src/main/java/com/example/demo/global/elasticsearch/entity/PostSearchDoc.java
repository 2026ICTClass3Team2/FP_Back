package com.example.demo.global.elasticsearch.entity;

import com.example.demo.domain.content.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.LocalDateTime;
import java.util.List;

@Document(indexName = "posts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setting(replicas = 0)
public class PostSearchDoc {

    @Id
    private String id; // ES prefers String IDs, we will parse your Long post_id to a String.

    @Field(type = FieldType.Text, analyzer = "nori") // Nori for Korean text
    private String title;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String body;

    @Field(type = FieldType.Keyword)
    private String authorName;

    @Field(type = FieldType.Keyword)
    private String contentType; // "feed" or "qna"

    @Field(type = FieldType.Boolean)
    private Boolean isSolved;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Date, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSS||uuuu-MM-dd'T'HH:mm:ss||uuuu-MM-dd")
    private LocalDateTime createdAt;

    @Field(type = FieldType.Integer)
    private Integer likeCount;

    @Field(type = FieldType.Keyword)
    private String thumbnailUrl;

    // Constructor to easily map from your JPA Entity to this ES Document
    public PostSearchDoc(Post post, List<String> tagNames) {
        this.id = post.getId().toString();
        this.title = post.getTitle();
        this.body = post.getBody();
        this.authorName = post.getAuthorName() != null ? post.getAuthorName() : post.getAuthor().getNickname();
        this.contentType = post.getContentType();
        this.isSolved = post.getIsSolved();
        this.tags = tagNames; // Pass in the flattened strings
        this.createdAt = post.getCreatedAt();
        this.likeCount = post.getLikeCount();
        this.thumbnailUrl = post.getThumbnailUrl();
    }
}

