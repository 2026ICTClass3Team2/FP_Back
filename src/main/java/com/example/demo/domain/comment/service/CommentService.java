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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final InteractionRepository interactionRepository;

    @Transactional
    public CommentResponseDto createComment(Long postId, CommentRequestDto requestDto, String username) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        User user = userRepository.findByUsername(username)
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
    public CommentResponseDto updateComment(Long commentId, CommentRequestDto requestDto, String username) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
        
        if (comment.getAuthor() == null || !comment.getAuthor().getUsername().equals(username)) {
            throw new IllegalArgumentException("Unauthorized to modify this comment");
        }
        if ("deleted".equals(comment.getStatus())) {
            throw new IllegalArgumentException("Cannot update a deleted comment");
        }

        comment.setContent(requestDto.getContent());
        return new CommentResponseDto(comment);
    }

    @Transactional
    public void deleteComment(Long commentId, String username) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        if (comment.getAuthor() == null || !comment.getAuthor().getUsername().equals(username)) {
            throw new IllegalArgumentException("Unauthorized to delete this comment");
        }

        comment.setStatus("deleted");
    }

    @Transactional
    public void toggleInteraction(Long commentId, String actionType, String username) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Optional<Interaction> existingInteraction = interactionRepository
                .findByUserIdAndTargetTypeAndTargetId(user.getId(), "comments", comment.getId());

        if (existingInteraction.isPresent()) {
            Interaction interaction = existingInteraction.get();
            if (interaction.getActionType().equals(actionType)) {
                // Toggle off
                interactionRepository.delete(interaction);
                updateCommentCount(comment, actionType, -1);
            } else {
                // Change action type (e.g., from like to dislike)
                interactionRepository.delete(interaction);
                updateCommentCount(comment, interaction.getActionType(), -1);
                
                Interaction newInteraction = Interaction.builder()
                        .user(user)
                        .targetId(comment.getId())
                        .targetType("comments")
                        .actionType(actionType)
                        .build();
                interactionRepository.save(newInteraction);
                updateCommentCount(comment, actionType, 1);
            }
        } else {
            // New interaction
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
            comment.setLikeCount(comment.getLikeCount() + delta);
        } else if ("dislike".equals(actionType)) {
            comment.setDislikeCount(comment.getDislikeCount() + delta);
        }
    }
}
