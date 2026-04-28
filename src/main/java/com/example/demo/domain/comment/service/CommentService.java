package com.example.demo.domain.comment.service;

import com.example.demo.domain.comment.dto.CommentRequestDto;
import com.example.demo.domain.comment.dto.CommentResponseDto;
import com.example.demo.domain.comment.entity.Comment;
import com.example.demo.domain.comment.repository.CommentRepository;
import com.example.demo.domain.content.entity.Post;
import com.example.demo.domain.content.repository.PostRepository;
import com.example.demo.domain.interaction.entity.Interaction;
import com.example.demo.domain.interaction.repository.InteractionRepository;
import com.example.demo.domain.report.enums.HiddenTargetType;
import com.example.demo.domain.report.repository.BlockRepository;
import com.example.demo.domain.report.repository.HiddenRepository;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final InteractionRepository interactionRepository;
    private final BlockRepository blockRepository;
    private final HiddenRepository hiddenRepository;
    private final com.example.demo.domain.notification.service.NotificationService notificationService;
    private final com.example.demo.domain.algorithm.service.UserInterestService userInterestService;

    @Transactional
    public CommentResponseDto createComment(Long postId, CommentRequestDto requestDto, String email) {
        log.info("Creating comment for post {} by user {}", postId, email);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if ("hidden".equals(post.getStatus()) || "deleted".equals(post.getStatus())) {
            throw new IllegalArgumentException("Post not found");
        }

        if ("frozen".equals(post.getStatus())) {
            throw new IllegalArgumentException("이 게시물은 동결되어 댓글을 작성할 수 없습니다.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Comment parent = null;
        if (requestDto.getParentId() != null) {
            parent = commentRepository.findById(requestDto.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent comment not found"));
            if (!"active".equals(parent.getStatus())) {
                throw new IllegalArgumentException("Parent comment not found or not active");
            }
            if (parent.getPost() == null || !postId.equals(parent.getPost().getId())) {
                throw new IllegalArgumentException("Parent comment does not belong to the requested post");
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

        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);

        // 관심도 반영 (피드 게시글 댓글만)
        if ("feed".equals(post.getContentType())) {
            userInterestService.onComment(user.getId(), postId);
        }

        // --- Notification Logic ---
        if (parent != null) {
            // It's a reply: notify the parent comment author
            if (parent.getAuthor() != null && !parent.getAuthor().getId().equals(user.getId())) {
                String message = "댓글에 새로운 답글이 달렸습니다: " + user.getNickname();
                notificationService.sendNotification(
                    parent.getAuthor(), 
                    "new reply", 
                    com.example.demo.domain.notification.entity.NotificationTargetType.comment, 
                    savedComment.getId(), 
                    message
                );
            }
        } else {
            // It's a root comment: notify the post author
            if (post.getAuthor() != null && !post.getAuthor().getId().equals(user.getId())) {
                String message = "게시글에 새로운 댓글이 달렸습니다: " + user.getNickname();
                notificationService.sendNotification(
                    post.getAuthor(), 
                    "new comment", 
                    com.example.demo.domain.notification.entity.NotificationTargetType.comment, 
                    savedComment.getId(), 
                    message
                );
            }
        }

        // Mention Detection
        String plainContent = requestDto.getContent().replaceAll("<[^>]*>", "");
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("@([^\\s@]+)");
        java.util.regex.Matcher matcher = pattern.matcher(plainContent);
        java.util.Set<String> mentionedNicknames = new java.util.HashSet<>();
        while (matcher.find()) {
            mentionedNicknames.add(matcher.group(1));
        }

        for (String nickname : mentionedNicknames) {
            userRepository.findByNickname(nickname).ifPresent(mentionedUser -> {
                if (!mentionedUser.getId().equals(user.getId())) {
                    String message = user.getNickname() + "님이 댓글에서 당신을 언급했습니다";
                    notificationService.sendNotification(
                        mentionedUser,
                        "mention",
                        com.example.demo.domain.notification.entity.NotificationTargetType.comment,
                        savedComment.getId(),
                        message
                    );
                }
            });
        }
        // ---------------------------

        return new CommentResponseDto(savedComment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDto> getComments(Long postId, String currentUserEmail) {
        // 1. 부모 댓글 조회 (최신순)
        List<Comment> rootComments = commentRepository.findRootCommentsByPostId(postId);
        if (rootComments.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 현재 사용자의 차단/숨김 정보 조회
        Set<Long> blockedUserIds = Collections.emptySet();
        Set<Long> hiddenCommentIds = Collections.emptySet();

        if (currentUserEmail != null) {
            User currentUser = userRepository.findByEmail(currentUserEmail).orElse(null);
            if (currentUser != null) {
                blockedUserIds = new HashSet<>(blockRepository.findBlockedUserIdsByBlockerId(currentUser.getId()));
                hiddenCommentIds = new HashSet<>(hiddenRepository.findTargetIdsByUserIdAndTargetType(currentUser.getId(), HiddenTargetType.comment));
            }
        }

        // 3. 부모 댓글 ID 리스트 추출
        List<Long> rootCommentIds = rootComments.stream()
                .map(Comment::getId)
                .collect(Collectors.toList());

        // 4. 대댓글 조회 (등록순)
        List<Comment> replies = commentRepository.findRepliesByParentIds(rootCommentIds);

        final Set<Long> finalBlockedUserIds = blockedUserIds;
        final Set<Long> finalHiddenCommentIds = hiddenCommentIds;

        // 5. DTO 변환 및 트리 구조 조립
        Map<Long, CommentResponseDto> commentMap = new HashMap<>();
        List<CommentResponseDto> rootCommentDtos = rootComments.stream()
                .map(comment -> {
                    CommentResponseDto dto = new CommentResponseDto(comment);
                    if (finalHiddenCommentIds.contains(comment.getId())) {
                        dto.setReported(true);
                    }
                    commentMap.put(dto.getId(), dto);
                    return dto;
                })
                .collect(Collectors.toList());

        for (Comment reply : replies) {
            // 차단된 유저의 대댓글 제외
            if (reply.getAuthor() != null && finalBlockedUserIds.contains(reply.getAuthor().getId())) {
                continue;
            }
            CommentResponseDto parentDto = commentMap.get(reply.getParent().getId());
            if (parentDto != null) {
                if (!"deleted".equals(reply.getStatus())) {
                    CommentResponseDto replyDto = new CommentResponseDto(reply);
                    if (finalHiddenCommentIds.contains(reply.getId())) {
                        replyDto.setReported(true);
                    }
                    parentDto.getChildren().add(replyDto);
                }
            }
        }

        // 6. 최종 리스트 필터링 (차단 유저 + replyCount 설정)
        return rootCommentDtos.stream()
                .filter(dto -> {
                    // 차단된 유저의 루트 댓글 제외 (단, 삭제된 댓글은 남김)
                    if (!"deleted".equals(dto.getStatus())) {
                        Long authorId = dto.getAuthorUserId();
                        if (authorId != null && finalBlockedUserIds.contains(authorId)) {
                            return false;
                        }
                    }
                    return true;
                })
                .peek(dto -> dto.setReplyCount(dto.getChildren().size()))
                .filter(dto -> "active".equals(dto.getStatus()) ||
                               ("deleted".equals(dto.getStatus()) && dto.getReplyCount() > 0))
                .collect(Collectors.toList());
    }

    @Transactional
    public CommentResponseDto updateComment(Long postId, Long commentId, CommentRequestDto requestDto, String email) {
        Comment comment = getCommentByPost(postId, commentId);

        if (!"active".equals(comment.getStatus())) {
            throw new IllegalArgumentException("Comment not found or not active");
        }

        if (comment.getPost() != null && "frozen".equals(comment.getPost().getStatus())) {
            throw new IllegalArgumentException("이 게시물은 동결되어 댓글을 수정할 수 없습니다.");
        }

        if (comment.getAuthor() == null || !comment.getAuthor().getEmail().equals(email)) {
            throw new SecurityException("Unauthorized to modify this comment");
        }

        comment.setContent(requestDto.getContent());
        return new CommentResponseDto(comment);
    }

    @Transactional
    public void deleteComment(Long postId, Long commentId, String email) {
        Comment comment = getCommentByPost(postId, commentId);

        if (comment.getAuthor() == null || !comment.getAuthor().getEmail().equals(email)) {
            throw new SecurityException("Unauthorized to delete this comment");
        }

        if (comment.getPost() != null && "frozen".equals(comment.getPost().getStatus())) {
            throw new IllegalArgumentException("이 게시물은 동결되어 댓글을 삭제할 수 없습니다.");
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
    public void toggleInteraction(Long postId, Long commentId, String actionType, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Comment comment = getCommentByPost(postId, commentId);

        if (!"active".equals(comment.getStatus())) {
            throw new IllegalArgumentException("Comment not found or not active");
        }

        if (comment.getPost() != null && "frozen".equals(comment.getPost().getStatus())) {
            throw new IllegalArgumentException("이 게시물은 동결되어 상호작용할 수 없습니다.");
        }

        Optional<Interaction> existingInteraction = interactionRepository
                .findByUserIdAndTargetTypeAndTargetId(user.getId(), "comment", comment.getId());

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
                    .targetType("comment")
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

    private Comment getCommentByPost(Long postId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        if (comment.getPost() == null || !postId.equals(comment.getPost().getId())) {
            throw new IllegalArgumentException("Comment does not belong to the requested post");
        }

        return comment;
    }
}
