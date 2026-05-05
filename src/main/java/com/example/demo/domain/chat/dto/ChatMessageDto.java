package com.example.demo.domain.chat.dto;

import com.example.demo.domain.chat.entity.ChatMessage;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * ChatMessageDto — 단일 채팅 메시지를 클라이언트에 전달하는 응답 DTO입니다.
 *
 * 사용처:
 *  1. ChatWebSocketHandler → 메시지 저장 후 송신자·수신자에게 실시간 푸시
 *  2. ChatController.getHistory() → REST API 이력 조회 응답
 *
 * 포함 데이터:
 *  - chatId, conversationId: 메시지·대화 식별
 *  - senderId, senderNickname, senderProfilePic: 송신자 표시용
 *  - receiverId: 수신자 식별
 *  - content: 메시지 본문
 *  - isRead: 읽음 여부
 *  - createdAt: 메시지 생성 시각
 */
@Getter
public class ChatMessageDto {

    /* ───── 식별 정보 ───── */
    private final Long chatId;
    private final Long conversationId;

    /* ───── 송신자 정보 ───── */
    private final Long senderId;
    private final String senderNickname;
    private final String senderProfilePic;

    /* ───── 수신자 정보 ───── */
    private final Long receiverId;

    /* ───── 메시지 내용 ───── */
    private final String content;
    private final Boolean isRead;
    private final boolean isEdited;
    private final boolean isDeleted;
    private final LocalDateTime createdAt;

    /**
     * WS 푸시 타입 식별자.
     * 클라이언트는 이 값으로 알림 WS 메시지와 채팅 WS 메시지를 구분합니다.
     * 값은 항상 "NEW_MESSAGE" 고정입니다.
     */
    private final String type = "NEW_MESSAGE";

    public ChatMessageDto(ChatMessage msg) {
        this.chatId          = msg.getId();
        this.conversationId  = msg.getConversationId();

        this.senderId        = msg.getSender() != null ? msg.getSender().getId() : null;
        this.senderNickname  = msg.getSender() != null ? msg.getSender().getNickname() : "(알 수 없음)";
        this.senderProfilePic = msg.getSender() != null ? msg.getSender().getProfilePicUrl() : null;

        this.receiverId      = msg.getReceiver() != null ? msg.getReceiver().getId() : null;

        this.isDeleted       = msg.isDeleted();
        this.content         = msg.isDeleted() ? null : msg.getContent();
        this.isRead          = msg.getIsRead();
        this.isEdited        = msg.isEdited();
        this.createdAt       = msg.getCreatedAt();
    }
}
