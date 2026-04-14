package com.example.demo.domain.comment.service;

import com.example.demo.domain.comment.dto.CommentRequestDto;
import com.example.demo.domain.comment.dto.CommentResponseDto;
import com.example.demo.domain.comment.entity.Comment;
import com.example.demo.domain.comment.enums.CommentStatus;
import com.example.demo.domain.comment.repository.CommentRepository;
import com.example.demo.domain.content.entity.Post;
import com.example.demo.domain.content.enums.PostStatus;
import com.example.demo.domain.content.feed.repository.PostRepository;
import com.example.demo.domain.interaction.entity.Interaction;
import com.example.demo.domain.interaction.repository.InteractionRepository;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final InteractionRepository interactionRepository;

    @Transactional
    public CommentResponseDto createComment(Long postId, CommentRequestDto requestDto, String email) {
        log.info("조회하려는 유저 이메일: {}", email);
        Post post = postRepository.findByIdAndStatus(postId, PostStatus.active)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Comment parent = null;
        if (requestDto.getParentId() != null) {
            parent = commentRepository.findByIdAndStatus(requestDto.getParentId(), CommentStatus.active)
                    .orElseThrow(() -> new IllegalArgumentException("Parent comment not found"));
        }

        Comment comment = Comment.builder()
                .content(requestDto.getContent())
                .post(post)
                .author(user)
                .parent(parent)
                .build();

        Comment savedComment = commentRepository.save(comment);

        post.setCommentCount(post.getCommentCount() + 1);

        return new CommentResponseDto(savedComment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDto> getComments(Long postId) {
        List<Comment> comments = commentRepository.findByPostIdWithAuthor(postId);
        Map<Long, CommentResponseDto> commentMap = new HashMap<>();

        for (Comment comment : comments) {
            commentMap.put(comment.getId(), new CommentResponseDto(comment));
        }

        for (Comment comment : comments) {
            if (comment.getParent() != null) {
                CommentResponseDto parentDto = commentMap.get(comment.getParent().getId());
                if (parentDto != null) {
                    CommentResponseDto childDto = commentMap.get(comment.getId());
                    if (childDto.getStatus().equals(CommentStatus.active.name())) {
                        parentDto.getChildren().add(childDto);
                    }
                }
            }
        }

        List<CommentResponseDto> rootComments = new ArrayList<>();
        for (Comment comment : comments) {
            if (comment.getParent() == null) {
                CommentResponseDto dto = commentMap.get(comment.getId());
                dto.setReplyCount(dto.getChildren().size());

                if (dto.getStatus().equals(CommentStatus.active.name()) || (dto.getStatus().equals(CommentStatus.deleted.name()) && dto.getReplyCount() > 0)) {
                    rootComments.add(dto);
                }
            }
        }

        return rootComments;
    }

    @Transactional
    public CommentResponseDto updateComment(Long commentId, CommentRequestDto requestDto, String email) {
        Comment comment = commentRepository.findByIdAndStatus(commentId, CommentStatus.active)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
        
        if (comment.getAuthor() == null || !comment.getAuthor().getEmail().equals(email)) {
            throw new IllegalArgumentException("Unauthorized to modify this comment");
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

        if (comment.getStatus() == CommentStatus.deleted) {
            // 이미 삭제된 댓글이면 아무것도 하지 않음
            return;
        }

        comment.setStatus(CommentStatus.deleted);

        Post post = comment.getPost();
        post.setCommentCount(Math.max(0, post.getCommentCount() - 1));
    }

    @Transactional
    public void toggleInteraction(Long commentId, String actionType, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Comment comment = commentRepository.findByIdAndStatus(commentId, CommentStatus.active)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        Optional<Interaction> existingInteraction = interactionRepository
                .findByUserIdAndTargetTypeAndTargetId(user.getId(), "comments", comment.getId());

        if (existingInteraction.isPresent()) {
            Interaction interaction = existingInteraction.get();
            if (interaction.getActionType().equals(actionType)) {
                interactionRepository.delete(interaction);
                updateCommentCount(comment, actionType, -1);
            } else {
                String previousActionType = interaction.getActionType();
                interaction.setActionType(actionType);
                updateCommentCount(comment, previousActionType, -1);
                updateCommentCount(comment, actionType, 1);
            }
        } else {
            Interaction newInteraction = Interaction.builder()
                    .user(user)
                    .targetId(comment.getId())
                    .targetType("comments")
                    .actionType(actionType)
                    .build();
            interactionRepository.save(newInteraction);
            updateCommentCount(comment, actionType, 1);
        }
    }

    private void updateCommentCount(Comment comment, String actionType, int delta) {
        if ("like".equals(actionType)) {
            comment.setLikeCount(Math.max(0, comment.getLikeCount() + delta));
        } else if ("dislike".equals(actionType)) {
            comment.setDislikeCount(Math.max(0, comment.getDislikeCount() + delta));
        }
    }
}
