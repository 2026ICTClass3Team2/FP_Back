package com.example.demo.domain.comment.service;

import com.example.demo.domain.comment.dto.CommentRequestDto;
import com.example.demo.domain.comment.dto.CommentResponseDto;
import com.example.demo.domain.comment.entity.Comment;
import com.example.demo.domain.comment.repository.CommentRepository;
import com.example.demo.domain.content.entity.Post;
import com.example.demo.domain.content.repository.PostRepository;
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
        log.info("Creating comment for post {} by user {}", postId, email);
        Post post = postRepository.findById(postId) // findByIdAndStatus 대신 findById 사용
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        
        if (!"active".equals(post.getStatus())) {
            throw new IllegalArgumentException("Post not found or not active");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Comment parent = null;
        if (requestDto.getParentId() != null) {
            parent = commentRepository.findById(requestDto.getParentId()) // findByIdAndStatus 대신 findById 사용
                    .orElseThrow(() -> new IllegalArgumentException("Parent comment not found"));
            if (!"active".equals(parent.getStatus())) {
                throw new IllegalArgumentException("Parent comment not found or not active");
            }
        }

        Comment comment = Comment.builder()
                .content(requestDto.getContent())
                .post(post)
                .author(user)
                .parent(parent)
                .status("active")
                .build();

        Comment savedComment = commentRepository.save(comment);

        // 댓글 수 증가 및 명시적 저장
        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);

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
                    if ("active".equals(childDto.getStatus())) {
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
                
                if ("active".equals(dto.getStatus()) || 
                   ("deleted".equals(dto.getStatus()) && dto.getReplyCount() > 0)) {
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
        
        if (!"active".equals(comment.getStatus())) {
            throw new IllegalArgumentException("Comment not found or not active");
        }
        
        if (comment.getAuthor() == null || !comment.getAuthor().getEmail().equals(email)) {
            throw new SecurityException("Unauthorized to modify this comment");
        }

        comment.setContent(requestDto.getContent());
        return new CommentResponseDto(comment);
    }

    @Transactional
    public void deleteComment(Long commentId, String email) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        if (comment.getAuthor() == null || !comment.getAuthor().getEmail().equals(email)) {
            throw new SecurityException("Unauthorized to delete this comment");
        }

        if ("deleted".equals(comment.getStatus())) {
            return;
        }

        comment.setStatus("deleted");

        Post post = comment.getPost();
        post.setCommentCount(Math.max(0, post.getCommentCount() - 1));
        postRepository.save(post);
    }

    @Transactional
    public void toggleInteraction(Long commentId, String actionType, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        if (!"active".equals(comment.getStatus())) {
            throw new IllegalArgumentException("Comment not found or not active");
        }

        Optional<Interaction> existingInteraction = interactionRepository
                .findByUserIdAndTargetTypeAndTargetId(user.getId(), "comments", comment.getId());

        if (existingInteraction.isPresent()) {
            Interaction interaction = existingInteraction.get();
            if (interaction.getActionType().equals(actionType)) {
                interactionRepository.delete(interaction);
                updateInteractionCount(comment, actionType, -1);
            } else {
                String previousActionType = interaction.getActionType();
                interaction.setActionType(actionType);
                updateInteractionCount(comment, previousActionType, -1);
                updateInteractionCount(comment, actionType, 1);
            }
        } else {
            Interaction newInteraction = Interaction.builder()
                    .user(user)
                    .targetId(comment.getId())
                    .targetType("comments")
                    .actionType(actionType)
                    .build();
            interactionRepository.save(newInteraction);
            updateInteractionCount(comment, actionType, 1);
        }
    }

    private void updateInteractionCount(Comment comment, String actionType, int delta) {
        if ("like".equals(actionType)) {
            comment.setLikeCount(Math.max(0, comment.getLikeCount() + delta));
        } else if ("dislike".equals(actionType)) {
            comment.setDislikeCount(Math.max(0, comment.getDislikeCount() + delta));
        }
    }
}
