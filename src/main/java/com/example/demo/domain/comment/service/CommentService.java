package com.example.demo.domain.comment.service;

import com.example.demo.domain.comment.dto.CommentRequestDto;
import com.example.demo.domain.comment.dto.CommentResponseDto;
import com.example.demo.domain.comment.entity.Comment;
import com.example.demo.domain.comment.repository.CommentRepository;
import com.example.demo.domain.content.feed.entity.Post;
import com.example.demo.domain.content.feed.repository.PostRepository;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommentResponseDto createComment(Long postId, CommentRequestDto requestDto, String email) {
        log.info("조회하려는 유저 이메일: {}", email);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Comment parent = null;
        if (requestDto.getParentId() != null) {
            parent = commentRepository.findById(requestDto.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent comment not found"));
        }

        Comment comment = Comment.builder()
                .content(requestDto.getContent())
                .post(post)
                .author(user)
                .parent(parent)
                .build();

        Comment savedComment = commentRepository.save(comment);
        return new CommentResponseDto(savedComment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDto> getComments(Long postId) {
        List<Comment> comments = commentRepository.findByPostIdWithAuthor(postId);
        List<CommentResponseDto> rootComments = new ArrayList<>();
        Map<Long, CommentResponseDto> commentMap = new HashMap<>();

        for (Comment comment : comments) {
            CommentResponseDto dto = new CommentResponseDto(comment);
            commentMap.put(comment.getId(), dto);

            if (comment.getParent() == null) {
                rootComments.add(dto);
            } else {
                CommentResponseDto parentDto = commentMap.get(comment.getParent().getId());
                if (parentDto != null) {
                    parentDto.getChildren().add(dto);
                } else {
                    rootComments.add(dto);
                }
            }
        }
        return rootComments;
    }

    @Transactional
    public CommentResponseDto updateComment(Long commentId, CommentRequestDto requestDto, String email) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
        
        if (comment.getAuthor() == null || !comment.getAuthor().getEmail().equals(email)) {
            throw new IllegalArgumentException("Unauthorized to modify this comment");
        }
        if ("deleted".equals(comment.getStatus())) {
            throw new IllegalArgumentException("Cannot update a deleted comment");
        }

        comment.setContent(requestDto.getContent());
        return new CommentResponseDto(comment);
    }

    @Transactional
    public void deleteComment(Long commentId, String email) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        if (comment.getAuthor() == null || !comment.getAuthor().getEmail().equals(email)) {
            throw new IllegalArgumentException("Unauthorized to delete this comment");
        }

        comment.setStatus("deleted");
    }

    @Transactional
    public void likeComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
        comment.setLikeCount(comment.getLikeCount() + 1);
    }

    @Transactional
    public void dislikeComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
        comment.setDislikeCount(comment.getDislikeCount() + 1);
    }
}
