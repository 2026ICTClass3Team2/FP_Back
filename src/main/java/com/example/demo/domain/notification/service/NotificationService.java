package com.example.demo.domain.notification.service;

import com.example.demo.domain.notification.dto.NotificationSettingDto;
import com.example.demo.domain.notification.entity.Notification;
import com.example.demo.domain.notification.entity.NotificationSetting;
import com.example.demo.domain.notification.entity.NotificationTargetType;
import com.example.demo.domain.notification.repository.NotificationRepository;
import com.example.demo.domain.notification.repository.NotificationSettingRepository;
import com.example.demo.domain.user.repository.UserRepository;
import com.example.demo.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final UserRepository userRepository;

    @Transactional
    public void sendNotification(User receiver, String type, NotificationTargetType targetType, Long targetId, String message) {
        // Check user settings
        NotificationSetting setting = notificationSettingRepository.findById(receiver.getId())
                .orElseGet(() -> {
                    NotificationSetting newSetting = NotificationSetting.builder().user(receiver).build();
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
                shouldSend = true; // Default to true for unknown types or system types
        }

        if (shouldSend) {
            Notification notification = Notification.builder()
                    .user(receiver)
                    .notificationType(message) // The user said "show a notification to the user 'new comment by 'nickname'"
                    .targetType(targetType)
                    .targetId(targetId)
                    .isRead(false)
                    .build();
            notificationRepository.save(notification);
        }
    }

    public List<Notification> getRecentUnread(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, 5));
    }

    @Transactional
    public void markAsRead(List<Long> notificationIds, String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        List<Notification> notifications = notificationRepository.findAllById(notificationIds);
        for (Notification n : notifications) {
            if (n.getUser().getId().equals(user.getId())) {
                n.setRead(true);
            }
        }
    }

    @Transactional
    public void markAllAsRead(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, 1000));
        for (Notification n : unread) {
            n.setRead(true);
        }
    }

    public List<Notification> getNotifications(String email, String filter) {
        User user = userRepository.findByEmail(email).orElseThrow();
        switch (filter) {
            case "new":
                return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(user.getId());
            case "read":
                return notificationRepository.findByUserIdAndIsReadTrueOrderByCreatedAtDesc(user.getId());
            default:
                return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        }
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
