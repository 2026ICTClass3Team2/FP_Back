package com.example.demo.global.websocket;

import com.example.demo.domain.chat.dto.ChatMessageDto;
import com.example.demo.domain.chat.dto.ChatSendRequest;
import com.example.demo.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * ChatWebSocketHandler — 실시간 1:1 채팅 메시지 라우팅을 담당합니다.
 *
 * 역할:
 *  - 연결된 사용자의 WebSocket 세션 관리
 *  - 클라이언트로부터 수신된 JSON 메시지 파싱
 *  - ChatService를 통한 메시지 영속화
 *  - 수신자(및 송신자의 다른 세션)에게 실시간 메시지 푸시
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatService chatService;
    private final ObjectMapper objectMapper; // JSON 파싱용

    // userId -> 활성 WebSocket 세션 집합
    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            closeQuietly(session);
            return;
        }
        userSessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
        log.info("[ChatWS] Connected. userId={}, sessionId={}", userId, session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) return;

        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
            }
        }
        log.info("[ChatWS] Closed. userId={}, sessionId={}", userId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long senderId = (Long) session.getAttributes().get("userId");
        if (senderId == null) return;

        try {
            // 1. 수신된 JSON 파싱
            ChatSendRequest request = objectMapper.readValue(message.getPayload(), ChatSendRequest.class);

            if ("SEND".equals(request.getType())) {
                // 2. DB 저장 및 DTO 생성
                ChatMessageDto messageDto = chatService.saveMessage(
                        senderId, request.getReceiverId(), request.getContent());

                // 3. 수신자에게 전송
                String payload = objectMapper.writeValueAsString(messageDto);
                sendMessageToUser(request.getReceiverId(), payload);

                // 4. 송신자 본인에게도 에코 (다른 탭 동기화 등)
                sendMessageToUser(senderId, payload);
            }
        } catch (Exception e) {
            log.error("[ChatWS] Error handling message: {}", e.getMessage(), e);
        }
    }

    /**
     * 특정 사용자에게 메시지를 전송합니다. (모든 활성 세션 대상)
     */
    public void sendMessageToUser(Long userId, String payload) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) return;

        TextMessage textMessage = new TextMessage(payload);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(textMessage);
                } catch (IOException e) {
                    log.warn("[ChatWS] Failed to send message to sessionId={}", session.getId());
                    sessions.remove(session);
                }
            } else {
                sessions.remove(session);
            }
        }
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            session.close(CloseStatus.POLICY_VIOLATION);
        } catch (IOException e) {
            log.warn("[ChatWS] Error closing session: {}", e.getMessage());
        }
    }
}
