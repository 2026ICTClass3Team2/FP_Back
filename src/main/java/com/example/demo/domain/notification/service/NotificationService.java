package com.example.demo.domain.notification.service;

import com.example.demo.domain.comment.entity.Comment;
import com.example.demo.domain.comment.repository.CommentRepository;
import com.example.demo.domain.notification.dto.NotificationResponseDto;
import com.example.demo.domain.notification.dto.NotificationSettingDto;
import com.example.demo.domain.notification.entity.Notification;
import com.example.demo.domain.notification.entity.NotificationSetting;
import com.example.demo.domain.notification.entity.NotificationTargetType;
import com.example.demo.domain.notification.repository.NotificationRepository;
import com.example.demo.domain.notification.repository.NotificationSettingRepository;
import com.example.demo.domain.qna.entity.Qna;
import com.example.demo.domain.qna.repository.QnaRepository;
import com.example.demo.domain.user.repository.UserRepository;
import com.example.demo.domain.user.entity.User;
import com.example.demo.global.websocket.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final QnaRepository qnaRepository;

    // WebSocket 핸들러 — 알림 저장 직후 실시간 푸시를 담당합니다.
    // @Lazy를 사용하는 이유: NotificationService와 WebSocketHandler 사이의
    // 순환 의존성 가능성을 방지하고 컨텍스트 초기화 순서 문제를 피하기 위함입니다.
    private final @Lazy NotificationWebSocketHandler notificationWebSocketHandler;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendNotification(User receiver, String type, NotificationTargetType targetType, Long targetId, String message) {
        // REQUIRES_NEW는 새 세션을 생성하므로 outer 트랜잭션의 receiver는 detached 상태.
        // getReferenceById(프록시)는 @MapsId persist 시 Hibernate가 ID를 추출할 때 null을 반환할 수 있어
        // AssertionFailure를 유발한다. findById로 완전 초기화된 엔티티를 사용한다.
        User managedReceiver = userRepository.findById(receiver.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + receiver.getId()));

        NotificationSetting setting = notificationSettingRepository.findById(receiver.getId())
                .orElseGet(() -> {
                    // userId는 명시하지 않음 — @MapsId가 user.getId()에서 자동으로 파생함.
                    // userId를 명시하면 save()가 merge()를 호출해 새 엔티티에서 AssertionFailure 재발.
                    NotificationSetting newSetting = NotificationSetting.builder()
                            .user(managedReceiver)
                            .build();
                    return notificationSettingRepository.save(newSetting);
                });

        boolean shouldSend = false;
        switch (type) {
            case "new post":
                if (targetType == NotificationTargetType.channel) shouldSend = setting.isFollowedChannel();
                else if (targetType == NotificationTargetType.user) shouldSend = setting.isFollowedUser();
                break;
            case "new comment":
                shouldSend = setting.isPostComment();
                break;
            case "new reply":
                shouldSend = setting.isCommentReply();
                break;
            case "qna selected":
                shouldSend = setting.isQnaAnswer();
                break;
            case "point":
                shouldSend = setting.isPointTransaction();
                break;
            case "mention":
                shouldSend = setting.isMention();
                break;
            case "chat":
                shouldSend = setting.isChat();
                break;
            case "admin":
                shouldSend = setting.isAdmin();
                break;
            default:
                shouldSend = true;
        }

        if (shouldSend) {
            Notification notification = Notification.builder()
                    .user(managedReceiver)
                    .notificationType(message)
                    .targetType(targetType)
                    .targetId(targetId)
                    .isRead(false)
                    .build();
            notificationRepository.save(notification);

            // DB 저장 성공 후 WebSocket으로 실시간 푸시 신호를 전송합니다.
            // 클라이언트는 이 신호를 받으면 /notifications/recent를 재조회합니다.
            // 페이로드는 최소한의 JSON으로 유지합니다 — 실제 데이터는 REST API로 가져옵니다.
            try {
                notificationWebSocketHandler.pushToUser(
                    receiver.getId(),
                    "{\"type\":\"NEW_NOTIFICATION\"}"
                );
            } catch (Exception e) {
                // WebSocket 푸시 실패는 치명적 오류가 아닙니다.
                // 알림은 이미 DB에 저장되었으므로 사용자는 다음 조회 시 확인할 수 있습니다.
                log.warn("[WS] 알림 푸시 실패 (DB 저장은 성공): userId={}, 오류={}",
                    receiver.getId(), e.getMessage());
            }
        }
    }

    public List<NotificationResponseDto> getRecentUnread(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        List<Notification> notifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, 5));
        return convertToDto(notifications);
    }

    private List<NotificationResponseDto> convertToDto(List<Notification> notifications) {
        return notifications.stream().map(n -> {
            Long postId = null;
            Long qnaId = null;
            if (n.getTargetType() == NotificationTargetType.comment || n.getTargetType() == NotificationTargetType.mention) {
                Comment comment = commentRepository.findById(n.getTargetId()).orElse(null);
                if (comment != null && comment.getPost() != null) {
                    postId = comment.getPost().getId();
                    if ("qna".equals(comment.getPost().getContentType())) {
                        Qna qna = qnaRepository.findByPostId(postId);
                        if (qna != null) qnaId = qna.getId();
                    }
                }
            } else if (n.getTargetType() == NotificationTargetType.post) {
                postId = n.getTargetId();
                Qna qna = qnaRepository.findByPostId(postId);
                if (qna != null) qnaId = qna.getId();
            }
            NotificationResponseDto dto = new NotificationResponseDto(n, postId);
            dto.setQnaId(qnaId);
            return dto;
        }).collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public void markAsRead(List<Long> notificationIds, String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        notificationRepository.markAsRead(notificationIds, user.getId());
    }

    @Transactional
    public void markAllAsRead(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        notificationRepository.markAllAsRead(user.getId());
    }

    @Transactional
    public void markTargetAsRead(User user, NotificationTargetType targetType, Long targetId) {
        notificationRepository.markTargetNotificationsAsRead(user.getId(), targetType, targetId);
    }

    @Transactional
    public void markAllTargetTypeAsRead(User user, NotificationTargetType targetType) {
        notificationRepository.markAllTargetTypeNotificationsAsRead(user.getId(), targetType);
    }
    public List<NotificationResponseDto> getNotifications(String email, String filter) {
        User user = userRepository.findByEmail(email).orElseThrow();
        List<Notification> notifications;
        switch (filter) {
            case "new":
                notifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(user.getId());
                break;
            case "read":
                notifications = notificationRepository.findByUserIdAndIsReadTrueOrderByCreatedAtDesc(user.getId());
                break;
            default:
                notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
                break;
        }
        return convertToDto(notifications);
    }

    public NotificationSettingDto getSettings(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        NotificationSetting setting = notificationSettingRepository.findById(user.getId())
                .orElseGet(() -> {
                    NotificationSetting newSetting = NotificationSetting.builder().user(user).build();
                    return notificationSettingRepository.save(newSetting);
                });
        return new NotificationSettingDto(setting);
    }

    @Transactional
    public void updateSettings(String email, NotificationSettingDto newSettings) {
        User user = userRepository.findByEmail(email).orElseThrow();
        NotificationSetting setting = notificationSettingRepository.findById(user.getId())
                .orElseGet(() -> NotificationSetting.builder().user(user).build());
        
        setting.setAdmin(newSettings.isAdmin());
        setting.setFollowedChannel(newSettings.isFollowedChannel());
        setting.setFollowedUser(newSettings.isFollowedUser());
        setting.setPostComment(newSettings.isPostComment());
        setting.setCommentReply(newSettings.isCommentReply());
        setting.setQnaAnswer(newSettings.isQnaAnswer());
        setting.setPointTransaction(newSettings.isPointTransaction());
        setting.setMention(newSettings.isMention());
        setting.setChat(newSettings.isChat());

        notificationSettingRepository.save(setting);
    }
}
