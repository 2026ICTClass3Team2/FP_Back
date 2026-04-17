package com.example.demo.domain.mypage.dto;

import com.example.demo.domain.content.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyPostDto {
    private Long id;
    private String contentType;
    private String title;
    private Integer likeCount;
    private Integer commentCount;
    private Integer viewCount;
    private LocalDateTime createdAt;

    public static MyPostDto from(Post post) {
        return MyPostDto.builder()
                .id(post.getId())
                .contentType(post.getContentType())
                .title(post.getTitle())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .viewCount(post.getViewCount())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
