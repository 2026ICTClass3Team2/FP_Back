package com.example.demo.domain.qna.service;

import com.example.demo.domain.comment.entity.Comment;
import com.example.demo.domain.comment.repository.CommentRepository;
import com.example.demo.domain.content.entity.Post;
import com.example.demo.domain.content.repository.PostRepository;
import com.example.demo.domain.notification.entity.NotificationTargetType;
import com.example.demo.domain.notification.service.NotificationService;
import com.example.demo.domain.point.entity.PointTransaction;
import com.example.demo.domain.point.repository.PointTransactionRepository;
import com.example.demo.domain.qna.entity.Qna;
import com.example.demo.domain.qna.repository.QnaRepository;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventQnaRewardService {

    private final QnaRepository qnaRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final NotificationService notificationService;

    @Transactional
    public void awardIfNotYetSolved(Long postId, Long commentId, Long commenterId) {
        Qna qna = qnaRepository.findByPostId(postId);
        if (qna == null || !qna.isEvent() || qna.isSolved() || qna.getEventPoints() <= 0) {
            return;
        }

        User commenter = userRepository.findById(commenterId).orElse(null);
        if (commenter == null) {
            return;
        }

        qna.setSolved(true);
        qnaRepository.save(qna);

        Post post = qna.getPost();
        if (post != null) {
            post.setIsSolved(true);
            postRepository.save(post);
        }

        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment != null) {
            comment.setIsAnswer(true);
            commentRepository.save(comment);
        }

        commenter.setCurrentPoint(commenter.getCurrentPoint() + qna.getEventPoints());
        userRepository.save(commenter);

        PointTransaction pointTx = PointTransaction.builder()
                .user(commenter)
                .targetId(commentId)
                .targetType("event")
                .pointChange(qna.getEventPoints())
                .pointBalance(commenter.getCurrentPoint())
                .build();
        pointTransactionRepository.save(pointTx);

        notificationService.sendNotification(
                commenter, "point", NotificationTargetType.system, commentId,
                "이벤트 QnA 정답 검증 완료! 포인트가 적립되었습니다: +" + qna.getEventPoints()
        );

        log.info("[이벤트 QnA] 포인트 지급 완료: postId={}, commentId={}, commenterId={}, points={}",
                postId, commentId, commenterId, qna.getEventPoints());
    }
}
