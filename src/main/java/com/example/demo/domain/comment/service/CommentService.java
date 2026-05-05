package com.example.demo.domain.comment.service;

import com.example.demo.domain.algorithm.service.UserInterestService;
import com.example.demo.domain.comment.dto.CommentRequestDto;
import com.example.demo.domain.comment.dto.CommentResponseDto;
import com.example.demo.domain.comment.entity.Comment;
import com.example.demo.domain.comment.repository.CommentRepository;
import com.example.demo.domain.content.entity.Post;
import com.example.demo.domain.content.repository.PostRepository;
import com.example.demo.domain.qna.entity.Qna;
import com.example.demo.domain.qna.repository.QnaRepository;
import com.example.demo.domain.interaction.entity.Interaction;
import com.example.demo.domain.interaction.repository.InteractionRepository;
import com.example.demo.domain.notification.entity.NotificationTargetType;
import com.example.demo.domain.notification.service.NotificationService;
import com.example.demo.domain.point.entity.PointTransaction;
import com.example.demo.domain.point.repository.PointTransactionRepository;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private final NotificationService notificationService;
    private final UserInterestService userInterestService;
    private final QnaRepository qnaRepository;
    private final PointTransactionRepository pointTransactionRepository;

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

        int preExistingCommentCount = post.getCommentCount();
        Comment savedComment = commentRepository.save(comment);

        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);

        // 관심도 반영 (피드 게시글 댓글만)
        if ("feed".equals(post.getContentType())) {
            userInterestService.onComment(user.getId(), postId);
        }

        // 이벤트 QnA 첫 댓글 자동 포인트 지급
        if (parent == null && preExistingCommentCount == 0 && "qna".equals(post.getContentType())) {
            try {
                Qna eventQna = qnaRepository.findByPostId(post.getId());
                if (eventQna != null && eventQna.isEvent() && !eventQna.isSolved()
                        && savedComment.getAuthor() != null && eventQna.getEventPoints() > 0) {
                    User commenter = savedComment.getAuthor();
                    commenter.setCurrentPoint(commenter.getCurrentPoint() + eventQna.getEventPoints());
                    userRepository.save(commenter);

                    PointTransaction pointTx = PointTransaction.builder()
                            .user(commenter)
                            .targetId(savedComment.getId())
                            .targetType("event")
                            .pointChange(eventQna.getEventPoints())
                            .pointBalance(commenter.getCurrentPoint())
                            .build();
                    pointTransactionRepository.save(pointTx);

                    notificationService.sendNotification(
                            commenter, "point", NotificationTargetType.system,
                            savedComment.getId(),
                            "이벤트 QnA 첫 답변으로 포인트가 적립되었습니다: +" + eventQna.getEventPoints()
                    );
                }
            } catch (Exception e) {
                log.warn("이벤트 포인트 지급 실패 (댓글 저장은 성공): commentId={}, 오류={}", savedComment.getId(), e.getMessage());
            }
        }

        // --- Notification Logic ---

        try {
            if (parent != null) {
                if (parent.getAuthor() != null && !parent.getAuthor().getId().equals(user.getId())) {
                    String message = "댓글에 새로운 답글이 달렸습니다: " + user.getNickname();
                    notificationService.sendNotification(
                            parent.getAuthor(),
                            "new reply",
                            NotificationTargetType.comment,
                            savedComment.getId(),
                            message
                    );
                }
            } else {
                if (post.getAuthor() != null && !post.getAuthor().getId().equals(user.getId())) {
                    String message = "게시글에 새로운 댓글이 달렸습니다: " + user.getNickname();
                    notificationService.sendNotification(
                            post.getAuthor(),
                            "new comment",
                            NotificationTargetType.comment,
                            savedComment.getId(),
                            message
                    );
                }
            }
        } catch (Exception e) {
            log.warn("알림 전송 실패 (댓글 저장은 성공): commentId={}, 오류={}", savedComment.getId(), e.getMessage());
        }

        // Mention Detection — parse embed spans first (supports spaces), then fall back to plain text
        Set<String> mentionedNicknames = new HashSet<>();
        Pattern htmlPattern = Pattern.compile("data-nickname=\"([^\"]+)\"");
        Matcher htmlMatcher = htmlPattern.matcher(requestDto.getContent());
        while (htmlMatcher.find()) {
            mentionedNicknames.add(htmlMatcher.group(1));
        }
        String plainContent = requestDto.getContent().replaceAll("<[^>]*>", "");
        Pattern pattern = Pattern.compile("@([가-힣a-zA-Z0-9._-]{2,50})");
        Matcher matcher = pattern.matcher(plainContent);
        while (matcher.find()) {
            mentionedNicknames.add(matcher.group(1));
        }

        for (String nickname : mentionedNicknames) {
            userRepository.findByNickname(nickname).ifPresent(mentionedUser -> {
                if (!mentionedUser.getId().equals(user.getId())) {
                    String message = user.getNickname() + "님이 댓글에서 당신을 언급했습니다";
                    try {
                        notificationService.sendNotification(
                                mentionedUser,
                                "mention",
                                NotificationTargetType.comment,
                                savedComment.getId(),
                                message
                        );
                    } catch (Exception e) {
                        log.warn("멘션 알림 전송 실패: commentId={}, nickname={}, 오류={}", savedComment.getId(), nickname, e.getMessage());
                    }
                }
            });
        }

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
        Set<Long> likedCommentIds = Collections.emptySet();
        Set<Long> dislikedCommentIds = Collections.emptySet();

        if (currentUserEmail != null) {
            User currentUser = userRepository.findByEmail(currentUserEmail).orElse(null);
            if (currentUser != null) {
                blockedUserIds = new HashSet<>(blockRepository.findBlockedUserIdsByBlockerId(currentUser.getId()));
                hiddenCommentIds = new HashSet<>(hiddenRepository.findTargetIdsByUserIdAndTargetType(currentUser.getId(), HiddenTargetType.comment));

                // 3. 부모 댓글 ID로 대댓글까지 포함한 전체 댓글 ID 수집 후 상호작용 일괄 조회
                List<Long> rootCommentIds = rootComments.stream().map(Comment::getId).collect(Collectors.toList());
                List<Comment> replies = commentRepository.findRepliesByParentIds(rootCommentIds);

                Set<Long> allCommentIds = new HashSet<>(rootCommentIds);
                replies.stream().map(Comment::getId).forEach(allCommentIds::add);

                List<Interaction> interactions = interactionRepository
                        .findByUserIdAndTargetTypeAndTargetIdIn(currentUser.getId(), "comment", allCommentIds);

                likedCommentIds = interactions.stream()
                        .filter(i -> "like".equals(i.getActionType()))
                        .map(Interaction::getTargetId)
                        .collect(Collectors.toSet());
                dislikedCommentIds = interactions.stream()
                        .filter(i -> "dislike".equals(i.getActionType()))
                        .map(Interaction::getTargetId)
                        .collect(Collectors.toSet());

                // replies를 재사용하기 위해 아래 조회를 대체 — 이미 조회했으므로 로컬 변수로 처리
                final Set<Long> finalBlockedUserIds = blockedUserIds;
                final Set<Long> finalHiddenCommentIds = hiddenCommentIds;
                final Set<Long> finalLikedCommentIds = likedCommentIds;
                final Set<Long> finalDislikedCommentIds = dislikedCommentIds;

                Map<Long, CommentResponseDto> commentMap = new HashMap<>();
                List<CommentResponseDto> rootCommentDtos = rootComments.stream()
                        .map(comment -> {
                            CommentResponseDto dto = new CommentResponseDto(comment);
                            if (finalHiddenCommentIds.contains(comment.getId())) dto.setReported(true);
                            dto.setLiked(finalLikedCommentIds.contains(comment.getId()));
                            dto.setDisliked(finalDislikedCommentIds.contains(comment.getId()));
                            commentMap.put(dto.getId(), dto);
                            return dto;
                        })
                        .collect(Collectors.toList());

                for (Comment reply : replies) {
                    if (reply.getAuthor() != null && finalBlockedUserIds.contains(reply.getAuthor().getId())) continue;
                    CommentResponseDto parentDto = commentMap.get(reply.getParent().getId());
                    if (parentDto != null && !"deleted".equals(reply.getStatus())) {
                        CommentResponseDto replyDto = new CommentResponseDto(reply);
                        if (finalHiddenCommentIds.contains(reply.getId())) replyDto.setReported(true);
                        replyDto.setLiked(finalLikedCommentIds.contains(reply.getId()));
                        replyDto.setDisliked(finalDislikedCommentIds.contains(reply.getId()));
                        parentDto.getChildren().add(replyDto);
                    }
                }

                return rootCommentDtos.stream()
                        .filter(dto -> {
                            if (!"deleted".equals(dto.getStatus())) {
                                Long authorId = dto.getAuthorUserId();
                                if (authorId != null && finalBlockedUserIds.contains(authorId)) return false;
                            }
                            return true;
                        })
                        .peek(dto -> dto.setReplyCount(dto.getChildren().size()))
                        .filter(dto -> "active".equals(dto.getStatus()) ||
                                       ("deleted".equals(dto.getStatus()) && dto.getReplyCount() > 0))
                        .collect(Collectors.toList());
            }
        }

        // 비로그인 사용자: 상호작용 상태 없이 반환
        List<Long> rootCommentIds = rootComments.stream().map(Comment::getId).collect(Collectors.toList());
        List<Comment> replies = commentRepository.findRepliesByParentIds(rootCommentIds);

        final Set<Long> finalBlockedUserIds = blockedUserIds;
        final Set<Long> finalHiddenCommentIds = hiddenCommentIds;

        Map<Long, CommentResponseDto> commentMap = new HashMap<>();
        List<CommentResponseDto> rootCommentDtos = rootComments.stream()
                .map(comment -> {
                    CommentResponseDto dto = new CommentResponseDto(comment);
                    if (finalHiddenCommentIds.contains(comment.getId())) dto.setReported(true);
                    commentMap.put(dto.getId(), dto);
                    return dto;
                })
                .collect(Collectors.toList());

        for (Comment reply : replies) {
            if (reply.getAuthor() != null && finalBlockedUserIds.contains(reply.getAuthor().getId())) continue;
            CommentResponseDto parentDto = commentMap.get(reply.getParent().getId());
            if (parentDto != null && !"deleted".equals(reply.getStatus())) {
                CommentResponseDto replyDto = new CommentResponseDto(reply);
                if (finalHiddenCommentIds.contains(reply.getId())) replyDto.setReported(true);
                parentDto.getChildren().add(replyDto);
            }
        }

        return rootCommentDtos.stream()
                .filter(dto -> {
                    if (!"deleted".equals(dto.getStatus())) {
                        Long authorId = dto.getAuthorUserId();
                        if (authorId != null && finalBlockedUserIds.contains(authorId)) return false;
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

        // Bug 5: 채택된 답변 댓글이 삭제되면 QnA 해결 상태 롤백
        if (Boolean.TRUE.equals(comment.getIsAnswer()) && "qna".equals(post.getContentType())) {
            Qna qna = qnaRepository.findByPostId(post.getId());
            if (qna != null && qna.isSolved()) {
                qna.setSolved(false);
                qna.setAnswerId(null);
                post.setIsSolved(false);
            }
        }
    }

    @Transactional
    public CommentResponseDto toggleInteraction(Long postId, Long commentId, String actionType, String email) {
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

        boolean nowLiked = false;
        boolean nowDisliked = false;

        if (existingInteraction.isPresent()) {
            Interaction interaction = existingInteraction.get();
            if (interaction.getActionType().equals(actionType)) {
                // 같은 액션 → 취소
                interactionRepository.delete(interaction);
                updateInteractionCount(comment, actionType, -1);
            } else {
                // 다른 액션으로 전환
                String previousActionType = interaction.getActionType();
                interaction.setActionType(actionType);
                updateInteractionCount(comment, previousActionType, -1);
                updateInteractionCount(comment, actionType, 1);
                nowLiked = "like".equals(actionType);
                nowDisliked = "dislike".equals(actionType);
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
            nowLiked = "like".equals(actionType);
            nowDisliked = "dislike".equals(actionType);
        }

        CommentResponseDto dto = new CommentResponseDto(comment);
        dto.setLiked(nowLiked);
        dto.setDisliked(nowDisliked);
        return dto;
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
