package com.example.demo.domain.chat.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * ConversationSummaryDto — 최근 대화 목록 한 항목을 나타내는 DTO입니다.
 *
 * 사용처:
 *  - ChatController.getConversations() REST 응답
 *  - 프론트엔드 DirectChatTab 좌측 패널 대화 목록 표시
 *
 * 포함 데이터:
 *  - conversationId : 대화 식별자
 *  - partnerId, partnerNickname, partnerProfilePic : 상대방 표시용
 *  - lastMessage    : 마지막 메시지 내용 (미리보기)
 *  - lastMessageAt  : 마지막 메시지 시각 (정렬·표시용)
 *  - unreadCount    : 현재 사용자 기준 읽지 않은 메시지 수
 */
@Getter
@Setter
@NoArgsConstructor
public class ConversationSummaryDto {

    private Long conversationId;

    /* ───── 상대방 정보 ───── */
    private Long partnerId;
    private String partnerNickname;
    private String partnerProfilePic;

    /* ───── 마지막 메시지 정보 ───── */
    private String lastMessage;
    private LocalDateTime lastMessageAt;

    /* ───── 읽지 않은 메시지 수 ───── */
    private long unreadCount;

    /**
     * 모든 필드를 받는 생성자.
     * ChatService에서 쿼리 결과로 직접 조립할 때 사용합니다.
     */
    public ConversationSummaryDto(
            Long conversationId,
            Long partnerId,
            String partnerNickname,
            String partnerProfilePic,
            String lastMessage,
            LocalDateTime lastMessageAt,
            long unreadCount
    ) {
        this.conversationId    = conversationId;
        this.partnerId         = partnerId;
        this.partnerNickname   = partnerNickname;
        this.partnerProfilePic = partnerProfilePic;
        this.lastMessage       = lastMessage;
        this.lastMessageAt     = lastMessageAt;
        this.unreadCount       = unreadCount;
    }
}
