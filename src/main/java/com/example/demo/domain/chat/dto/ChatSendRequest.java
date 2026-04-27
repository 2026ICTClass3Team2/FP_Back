package com.example.demo.domain.chat.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ChatSendRequest — 클라이언트가 WebSocket으로 전송하는 채팅 명령 DTO입니다.
 *
 * 클라이언트 → 서버 방향 메시지 형식 (JSON):
 * {
 *   "type"       : "SEND",      // 현재는 "SEND"만 사용
 *   "receiverId" : 42,          // 메시지를 받을 사용자 ID
 *   "content"    : "안녕하세요!" // 메시지 본문 (최대 TEXT 길이)
 * }
 *
 * 역할:
 *  - ChatWebSocketHandler.handleTextMessage()에서 Jackson으로 역직렬화합니다.
 *  - type 필드로 향후 READ_ACK 등 새로운 명령을 추가할 수 있도록 확장성을 확보합니다.
 */
@Getter
@Setter
@NoArgsConstructor
public class ChatSendRequest {

    /**
     * 메시지 타입.
     * "SEND" — 새 메시지 전송 요청
     * (향후 "READ_ACK" 등 확장 가능)
     */
    private String type;

    /**
     * 수신자의 user_id.
     * 서버는 이 값을 사용해 대화 ID를 결정하고 메시지를 저장합니다.
     */
    private Long receiverId;

    /**
     * 메시지 본문.
     * 빈 문자열이나 공백만 있는 경우 서버에서 무시합니다.
     */
    private String content;
}
