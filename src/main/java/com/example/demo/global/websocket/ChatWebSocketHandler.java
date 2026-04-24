package com.example.demo.global.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

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
        // Here we would parse the message and broadcast via Redis
        // For the "shell" implementation, we just log it.
        log.info("[ChatWS] Message from user: {}", message.getPayload());
    }

    public void sendMessageToUser(Long userId, String payload) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) return;

        TextMessage message = new TextMessage(payload);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(message);
                } catch (IOException e) {
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
