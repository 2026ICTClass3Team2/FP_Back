package com.example.demo.domain.notification.entity;

import com.example.demo.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_setting")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationSetting {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    @Builder.Default
    private boolean admin = true;

    @Column(name = "followed_channel", nullable = false)
    @Builder.Default
    private boolean followedChannel = true;

    @Column(name = "followed_user", nullable = false)
    @Builder.Default
    private boolean followedUser = true;

    @Column(name = "post_comment", nullable = false)
    @Builder.Default
    private boolean postComment = true;

    @Column(name = "comment_reply", nullable = false)
    @Builder.Default
    private boolean commentReply = true;

    @Column(name = "qna_answer", nullable = false)
    @Builder.Default
    private boolean qnaAnswer = true;

    @Column(name = "point_transaction", nullable = false)
    @Builder.Default
    private boolean pointTransaction = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean mention = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean chat = true;
}
