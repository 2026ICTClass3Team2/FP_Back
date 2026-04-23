package com.example.demo.domain.notification.dto;

import com.example.demo.domain.notification.entity.NotificationSetting;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationSettingDto {
    private boolean followedChannel;
    private boolean followedUser;
    private boolean postComment;
    private boolean commentReply;
    private boolean qnaAnswer;
    private boolean pointTransaction;
    private boolean mention;
    private boolean chat;
    private boolean admin;

    public NotificationSettingDto(NotificationSetting entity) {
        this.followedChannel = entity.isFollowedChannel();
        this.followedUser = entity.isFollowedUser();
        this.postComment = entity.isPostComment();
        this.commentReply = entity.isCommentReply();
        this.qnaAnswer = entity.isQnaAnswer();
        this.pointTransaction = entity.isPointTransaction();
        this.mention = entity.isMention();
        this.chat = entity.isChat();
        this.admin = entity.isAdmin();
    }
}
