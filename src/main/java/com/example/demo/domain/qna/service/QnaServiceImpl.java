package com.example.demo.domain.qna.service;

import com.example.demo.domain.content.entity.Bookmark;
import com.example.demo.domain.content.entity.ContentTag;
import com.example.demo.domain.content.entity.Post;
import com.example.demo.domain.content.entity.Tag;
import com.example.demo.domain.content.repository.BookmarkRepository;
import com.example.demo.domain.content.repository.ContentTagRepository;
import com.example.demo.domain.content.repository.PostRepository;
import com.example.demo.domain.content.repository.TagRepository;
import com.example.demo.domain.interaction.entity.Interaction;
import com.example.demo.domain.interaction.repository.InteractionRepository;
import com.example.demo.domain.notification.entity.NotificationTargetType;
import com.example.demo.domain.point.entity.PointTransaction;
import com.example.demo.domain.point.repository.PointTransactionRepository;
import com.example.demo.domain.comment.entity.Comment;
import com.example.demo.domain.comment.repository.CommentRepository;
import com.example.demo.domain.qna.dto.QnaCardResponseDto;
import com.example.demo.domain.qna.dto.QnaCreateRequestDto;
import com.example.demo.domain.qna.dto.QnaDetailResponseDto;
import com.example.demo.domain.qna.entity.Qna;
import com.example.demo.domain.qna.repository.QnaRepository;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QnaServiceImpl implements QnaService {

    private final PostRepository postRepository;
    private final QnaRepository qnaRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final ContentTagRepository contentTagRepository;
    private final InteractionRepository interactionRepository;
    private final BookmarkRepository bookmarkRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final CommentRepository commentRepository;
    private final com.example.demo.domain.notification.service.NotificationService notificationService;

    @Override
    @Transactional
    public void createQna(QnaCreateRequestDto qnaCreateRequestDto, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        int rewardPoints = qnaCreateRequestDto.getRewardPoints();
        if (rewardPoints > user.getCurrentPoint()) {
            throw new IllegalArgumentException("보유한 포인트보다 많은 포인트를 걸 수 없습니다.");
        }

        user.setCurrentPoint(user.getCurrentPoint() - rewardPoints);
        userRepository.save(user);

        Post post = Post.builder()
                .title(qnaCreateRequestDto.getTitle())
                .body(qnaCreateRequestDto.getBody())
                .contentType("qna")
                .author(user)
                .build();
        Post savedPost = postRepository.save(post);

        Qna qna = Qna.builder()
                .post(savedPost)
                .rewardPoints(rewardPoints)
                .build();
        Qna savedQna = qnaRepository.save(qna);

        if (rewardPoints > 0) {
            PointTransaction transaction = PointTransaction.builder()
                    .user(user)
                    .targetId(savedQna.getId())
                    .targetType("qna")
                    .pointChange(-rewardPoints)
                    .pointBalance(user.getCurrentPoint())
                    .build();
            pointTransactionRepository.save(transaction);
            
            // --- Notification Logic ---
            notificationService.sendNotification(user, "point", NotificationTargetType.system, savedQna.getId(), "QnA 보상 설정으로 포인트가 차감되었습니다: -" + rewardPoints);
            // ---------------------------
        }

        saveTags(savedPost, qnaCreateRequestDto.getTags());

        // Mentions
        processMentions(savedPost, user);
    }

    private void processMentions(Post post, User author) {
        if (post.getBody() == null) return;
        
        String plainContent = post.getBody().replaceAll("<[^>]*>", "");
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("@([가-힣a-zA-Z0-9]{2,10})");
        java.util.regex.Matcher matcher = pattern.matcher(plainContent);
        java.util.Set<String> mentionedNicknames = new java.util.HashSet<>();
        
        while (matcher.find()) {
            mentionedNicknames.add(matcher.group(1));
        }

        for (String nickname : mentionedNicknames) {
            userRepository.findByNickname(nickname).ifPresent(mentionedUser -> {
                if (!mentionedUser.getId().equals(author.getId())) {
                    String message = author.getNickname() + "님이 QnA에서 당신을 언급했습니다";
                    notificationService.sendNotification(
                        mentionedUser,
                        "mention",
                        NotificationTargetType.post, // QnA is a post with contentType='qna'
                        post.getId(),
                        message
                    );
                }
            });
        }
    }

    @Override
    @Transactional
    public void updateQna(Long qnaId, QnaCreateRequestDto qnaCreateRequestDto, String email) {
        Qna qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new IllegalArgumentException("Qna not found"));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Post post = qna.getPost();
        
        if (!"active".equals(post.getStatus())) {
            throw new IllegalArgumentException("이 질문은 수정할 수 없는 상태입니다. (동결 또는 삭제)");
        }
        
        if (post.getAuthor() == null || !post.getAuthor().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Not authorized to update this post");
        }

        int oldRewardPoints = qna.getRewardPoints();
        int newRewardPoints = qnaCreateRequestDto.getRewardPoints();
        int pointDifference = newRewardPoints - oldRewardPoints;

        if (pointDifference > 0) {
            if (pointDifference > user.getCurrentPoint()) {
                throw new IllegalArgumentException("보유한 포인트보다 많은 포인트를 걸 수 없습니다.");
            }
            user.setCurrentPoint(user.getCurrentPoint() - pointDifference);
            userRepository.save(user);

            PointTransaction transaction = PointTransaction.builder()
                    .user(user)
                    .targetId(qna.getId())
                    .targetType("qna")
                    .pointChange(-pointDifference)
                    .pointBalance(user.getCurrentPoint())
                    .build();
            pointTransactionRepository.save(transaction);

            // --- Notification Logic ---
            notificationService.sendNotification(user, "point", NotificationTargetType.system, qna.getId(), "QnA 보상 수정으로 포인트가 차감되었습니다: -" + pointDifference);
            // ---------------------------
        } else if (pointDifference < 0) {
            // If they reduced the points, refund the difference
            int refund = Math.abs(pointDifference);
            user.setCurrentPoint(user.getCurrentPoint() + refund);
            userRepository.save(user);

            PointTransaction transaction = PointTransaction.builder()
                    .user(user)
                    .targetId(qna.getId())
                    .targetType("qna")
                    .pointChange(refund)
                    .pointBalance(user.getCurrentPoint())
                    .build();
            pointTransactionRepository.save(transaction);

            // --- Notification Logic ---
            notificationService.sendNotification(user, "point", NotificationTargetType.system, qna.getId(), "QnA 보상 수정으로 포인트가 환불되었습니다: +" + refund);
        }

        post.setTitle(qnaCreateRequestDto.getTitle());
        post.setBody(qnaCreateRequestDto.getBody());
        qna.setRewardPoints(newRewardPoints);

        // Update tags: Clear existing and save new ones
        contentTagRepository.deleteAll(post.getContentTags());
        post.getContentTags().clear();
        saveTags(post, qnaCreateRequestDto.getTags());

        // Mentions on update
        processMentions(post, user);
    }

    private void saveTags(Post post, List<String> tagNames) {
        if (tagNames != null && !tagNames.isEmpty()) {
            for (String tagName : tagNames) {
                Tag tag = tagRepository.findByName(tagName)
                        .orElseGet(() -> tagRepository.save(Tag.builder().name(tagName).build()));
                ContentTag contentTag = ContentTag.builder()
                        .post(post)
                        .tag(tag)
                        .build();
                contentTagRepository.save(contentTag);
            }
        }
    }

    @Transactional
    public void acceptAnswer(Long qnaId, Long commentId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Qna qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new IllegalArgumentException("Qna not found"));

        if (!qna.getPost().getAuthor().getId().equals(user.getId())) {
            throw new IllegalArgumentException("질문 작성자만 답변을 채택할 수 있습니다.");
        }

        if (!"active".equals(qna.getPost().getStatus())) {
            throw new IllegalArgumentException("동결되거나 삭제된 질문에서는 답변을 채택할 수 없습니다.");
        }

        if (qna.isSolved()) {
            throw new IllegalArgumentException("이미 채택된 질문입니다.");
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        // 채택할 댓글이 해당 QnA 게시글에 속하는지 확인합니다.
        // 이 검사가 없으면 다른 게시글의 댓글 ID를 전달해 잘못된 포인트 지급이나
        // 엉뚱한 알림 전송이 발생할 수 있습니다.
        if (!comment.getPost().getId().equals(qna.getPost().getId())) {
            throw new IllegalArgumentException("해당 QnA에 속하지 않는 댓글은 채택할 수 없습니다.");
        }

        // 루트 댓글(최상위 댓글)만 채택할 수 있습니다. 대댓글은 채택 대상이 아닙니다.
        if (comment.getParent() != null) {
            throw new IllegalArgumentException("대댓글은 답변으로 채택할 수 없습니다.");
        }

        if (comment.getAuthor() != null && comment.getAuthor().getId().equals(user.getId())) {
            throw new IllegalArgumentException("자신의 답변은 채택할 수 없습니다.");
        }

        // 1. Update states
        qna.setSolved(true);
        qna.setAnswerId(comment);
        qnaRepository.save(qna);

        Post post = qna.getPost();
        post.setIsSolved(true);
        postRepository.save(post);

        comment.setIsAnswer(true);
        commentRepository.save(comment);

        // 2. Give points to the comment author
        if (qna.getRewardPoints() > 0 && comment.getAuthor() != null) {
            User commentAuthor = comment.getAuthor();
            commentAuthor.setCurrentPoint(commentAuthor.getCurrentPoint() + qna.getRewardPoints());
            userRepository.save(commentAuthor);

            PointTransaction transaction = PointTransaction.builder()
                    .user(commentAuthor)
                    .targetId(comment.getId())
                    .targetType("comment")
                    .pointChange(qna.getRewardPoints())
                    .pointBalance(commentAuthor.getCurrentPoint())
                    .build();
            pointTransactionRepository.save(transaction);

            // Notify about points
            notificationService.sendNotification(commentAuthor, "point", NotificationTargetType.system, comment.getId(), "답변 채택으로 포인트가 적립되었습니다: +" + qna.getRewardPoints());
        }

        // 3. Notify about selection (Always)
        if (comment.getAuthor() != null) {
            notificationService.sendNotification(comment.getAuthor(), "qna selected", NotificationTargetType.comment, comment.getId(), "작성하신 댓글이 답변으로 채택되었습니다!");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QnaCardResponseDto> getQnaList(String query, String sort, String status, int page, int size, String email) {
        Pageable pageable = PageRequest.of(page, size);
        Page<QnaCardResponseDto> results = qnaRepository.findQnaList(query, sort, status, pageable);
        
        if (email != null) {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                Long userId = user.getId();
                results.getContent().forEach(dto -> {
                    // Populate tech stacks for the list view
                    Qna qna = qnaRepository.findById(dto.getQnaId()).orElse(null);
                    if(qna != null) {
                        dto.setPostId(qna.getPost().getId());
                        List<String> techStacks = qna.getPost().getContentTags().stream()
                                .map(contentTag -> contentTag.getTag().getName())
                                .collect(Collectors.toList());
                        dto.setTechStacks(techStacks);

                        dto.setAuthor(qna.getPost().getAuthor() != null && qna.getPost().getAuthor().getId().equals(userId));
                        dto.setBookmarked(bookmarkRepository.existsByUserIdAndTargetIdAndTargetType(userId, qna.getPost().getId(), "qna"));

                        Optional<Interaction> interaction = interactionRepository.findByUserIdAndTargetTypeAndTargetId(userId, "post", qna.getPost().getId());
                        if (interaction.isPresent()) {
                            String actionType = interaction.get().getActionType();
                            dto.setLiked("like".equals(actionType));
                            dto.setDisliked("dislike".equals(actionType));
                        }
                    }
                });
            }
        } else {
            // Populate tech stacks even for unauthenticated users
            results.getContent().forEach(dto -> {
                Qna qna = qnaRepository.findById(dto.getQnaId()).orElse(null);
                if(qna != null) {
                    List<String> techStacks = qna.getPost().getContentTags().stream()
                            .map(contentTag -> contentTag.getTag().getName())
                            .collect(Collectors.toList());
                    dto.setTechStacks(techStacks);
                }
            });
        }
        
        return results;
    }

    @Override
    @Transactional
    public QnaDetailResponseDto getQnaDetail(Long qnaId, String email) {
        Qna qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new IllegalArgumentException("Qna not found"));
        Post post = qna.getPost();
        if (post == null || post.getId() == null) {
            throw new IllegalArgumentException("Qna post not found");
        }
        if (!"active".equals(post.getStatus()) && !"frozen".equals(post.getStatus())) {
            throw new IllegalArgumentException("Qna not found");
        }

        // Increase view count
        Long userId = null;
        if (email != null) {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                userId = user.getId();
            }
        }
        postRepository.increaseViewCount(post.getId()); // Directly increment view count in DB

        // increaseViewCount uses a bulk update that clears the persistence context.
        // Re-load Qna/Post so lazy associations (author/contentTags) are attached to the current session.
        qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new IllegalArgumentException("Qna not found"));
        post = qna.getPost();
        if (post == null || post.getId() == null) {
            throw new IllegalArgumentException("Qna post not found");
        }
        if (!"active".equals(post.getStatus()) && !"frozen".equals(post.getStatus())) {
            throw new IllegalArgumentException("Qna not found");
        }

        User author = post.getAuthor();
        
        QnaDetailResponseDto dto = new QnaDetailResponseDto();
        dto.setQnaId(qna.getId());
        dto.setPostId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setBody(post.getBody());
        
        if (author != null) {
            dto.setUsername(author.getUsername());
            dto.setNickname(author.getNickname());
            dto.setAuthorProfileImageUrl(author.getProfilePicUrl());
            dto.setUserId(author.getId());
        } else {
            // Handle case where author is null for old data
            dto.setUsername("unknown");
            dto.setNickname("익명");
            dto.setAuthorProfileImageUrl(null);
            dto.setUserId(null);
        }

        dto.setResolved(qna.isSolved());
        dto.setPoints(qna.getRewardPoints());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setCommentCount(post.getCommentCount());
        dto.setLikeCount(post.getLikeCount());
        dto.setDislikeCount(post.getDislikeCount());
        dto.setViewCount(post.getViewCount()); // post proxy is initialized from DB after the @Modifying UPDATE, so value is already N+1

        List<String> techStacks = post.getContentTags().stream()
                .map(contentTag -> contentTag.getTag().getName())
                .collect(Collectors.toList());
        dto.setTechStacks(techStacks);

        if (userId != null && author != null) {
            dto.setAuthor(author.getId().equals(userId));
        }

        if (userId != null) {
            dto.setBookmarked(bookmarkRepository.existsByUserIdAndTargetIdAndTargetType(userId, post.getId(), "qna"));
            
            Optional<Interaction> interaction = interactionRepository.findByUserIdAndTargetTypeAndTargetId(userId, "post", post.getId());
            if (interaction.isPresent()) {
                String actionType = interaction.get().getActionType();
                dto.setLiked("like".equals(actionType));
                dto.setDisliked("dislike".equals(actionType));
            }
        }

        return dto;
    }
    
    @Override
    @Transactional
    public void deleteQna(Long qnaId, String email) {
        Qna qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new IllegalArgumentException("Qna not found"));
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
                
        if (qna.getPost().getAuthor() == null || !qna.getPost().getAuthor().getId().equals(user.getId())) {
             throw new IllegalArgumentException("Not authorized to delete this post");
        }

        if (!"active".equals(qna.getPost().getStatus())) {
            throw new IllegalArgumentException("이미 삭제되었거나 동결된 게시물은 삭제할 수 없습니다.");
        }

        qna.getPost().setStatus("hidden");
    }

    @Override
    @Transactional
    public void toggleLike(Long qnaId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Qna qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new IllegalArgumentException("Qna not found"));
        Post post = qna.getPost();

        if (!"active".equals(post.getStatus())) {
            throw new IllegalArgumentException("동결되거나 삭제된 질문에는 반응할 수 없습니다.");
        }

        Optional<Interaction> existingInteraction = interactionRepository.findByUserIdAndTargetTypeAndTargetId(user.getId(), "post", post.getId());

        if (existingInteraction.isPresent()) {
            Interaction interaction = existingInteraction.get();
            if ("like".equals(interaction.getActionType())) {
                // Cancel like
                interactionRepository.delete(interaction);
                post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
            } else if ("dislike".equals(interaction.getActionType())) {
                // Change dislike to like
                interaction.setActionType("like");
                interactionRepository.save(interaction);
                post.setDislikeCount(Math.max(0, post.getDislikeCount() - 1));
                post.setLikeCount(post.getLikeCount() + 1);
            }
        } else {
            // New like
            Interaction interaction = Interaction.builder()
                    .user(user)
                    .targetType("post")
                    .targetId(post.getId())
                    .actionType("like")
                    .build();
            interactionRepository.save(interaction);
            post.setLikeCount(post.getLikeCount() + 1);
        }
    }

    @Override
    @Transactional
    public void toggleDislike(Long qnaId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Qna qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new IllegalArgumentException("Qna not found"));
        Post post = qna.getPost();

        if (!"active".equals(post.getStatus())) {
            throw new IllegalArgumentException("동결되거나 삭제된 질문에는 반응할 수 없습니다.");
        }

        Optional<Interaction> existingInteraction = interactionRepository.findByUserIdAndTargetTypeAndTargetId(user.getId(), "post", post.getId());

        if (existingInteraction.isPresent()) {
            Interaction interaction = existingInteraction.get();
            if ("dislike".equals(interaction.getActionType())) {
                // Cancel dislike
                interactionRepository.delete(interaction);
                post.setDislikeCount(Math.max(0, post.getDislikeCount() - 1));
            } else if ("like".equals(interaction.getActionType())) {
                // Change like to dislike
                interaction.setActionType("dislike");
                interactionRepository.save(interaction);
                post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
                post.setDislikeCount(post.getDislikeCount() + 1);
            }
        } else {
            // New dislike
            Interaction interaction = Interaction.builder()
                    .user(user)
                    .targetType("post")
                    .targetId(post.getId())
                    .actionType("dislike")
                    .build();
            interactionRepository.save(interaction);
            post.setDislikeCount(post.getDislikeCount() + 1);
        }
    }

    @Override
    @Transactional
    public void toggleBookmark(Long qnaId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check if QnA exists
        Qna qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new IllegalArgumentException("Qna not found"));

        Post post = qna.getPost();
        if (!"active".equals(post.getStatus())) {
            throw new IllegalArgumentException("동결되거나 삭제된 질문은 북마크할 수 없습니다.");
        }

        Optional<Bookmark> existingBookmark = bookmarkRepository.findByUserIdAndTargetIdAndTargetType(user.getId(), post.getId(), "qna");

        if (existingBookmark.isPresent()) {
            // Unbookmark
            bookmarkRepository.delete(existingBookmark.get());
            postRepository.updateBookmarkCount(post.getId(), -1);
        } else {
            // Bookmark
            Bookmark bookmark = Bookmark.builder()
                    .user(user)
                    .targetId(post.getId())
                    .targetType("qna")
                    .build();
            bookmarkRepository.save(bookmark);
            postRepository.updateBookmarkCount(post.getId(), 1);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Long resolveQnaPostId(Long qnaIdentifier) {
        Qna qna = qnaRepository.findById(qnaIdentifier)
                .orElseThrow(() -> new IllegalArgumentException("Qna not found"));

        Post post = qna.getPost();
        if (post == null || post.getId() == null) {
            throw new IllegalArgumentException("Qna post not found");
        }
        if (!"active".equals(post.getStatus())) {
            throw new IllegalArgumentException("Qna not found");
        }

        return post.getId();
    }
}

