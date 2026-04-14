package com.example.demo.domain.comment.dto;

import com.example.demo.domain.comment.entity.Comment;
import com.example.demo.domain.comment.enums.CommentStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CommentResponseDto {
    private Long id;
    private String content;
    private Boolean isAnswer;
    private Integer likeCount;
    private Integer dislikeCount;
    private String status;
    private LocalDateTime createdAt;
    private String authorNickname;
    private String authorProfilePicUrl;
    private Long parentId;
    private Integer replyCount = 0; // 대댓글 수
    private List<CommentResponseDto> children = new ArrayList<>();

    public CommentResponseDto(Comment comment) {
        this.id = comment.getId();
        if (CommentStatus.deleted == comment.getStatus()) {
            this.content = "삭제된 댓글입니다";
        } else {
            this.content = comment.getContent();
        }
        this.isAnswer = comment.getIsAnswer();
        this.likeCount = comment.getLikeCount();
        this.dislikeCount = comment.getDislikeCount();
        this.status = comment.getStatus().name();
        this.createdAt = comment.getCreatedAt();
        if (comment.getAuthor() != null) {
            this.authorNickname = comment.getAuthor().getNickname();
            this.authorProfilePicUrl = comment.getAuthor().getProfilePicUrl();
        }
        if (comment.getParent() != null) {
            this.parentId = comment.getParent().getId();
        }
    }
}
