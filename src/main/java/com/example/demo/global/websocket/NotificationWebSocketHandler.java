package com.example.demo.global.websocket;

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

/**
 * WebSocket 알림 핸들러입니다.
 *
 * 역할:
 *  - 사용자별 WebSocket 세션 목록을 유지 관리합니다.
 *  - 클라이언트가 연결/해제될 때 세션 맵을 업데이트합니다.
 *  - pushToUser()를 통해 특정 사용자의 모든 활성 세션에 메시지를 전송합니다.
 *
 * 세션 저장 구조:
 *  Map<userId, Set<WebSocketSession>>
 *  → 한 사용자가 여러 탭/브라우저로 동시에 접속해도 모두 처리됩니다.
 *
 * Thread-safety:
 *  - ConcurrentHashMap과 CopyOnWriteArraySet을 사용해 동시성 문제를 방지합니다.
 */
@Slf4j
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    // userId → 해당 사용자의 활성 WebSocket 세션 집합
    // 동일 사용자가 여러 탭에서 접속하는 상황을 지원합니다.
    private final Map<Long, Set<WebSocketSession>> userSessions =
            new ConcurrentHashMap<>();

    /**
     * 클라이언트가 WebSocket 연결을 성공적으로 맺었을 때 호출됩니다.
     *
     * JwtHandshakeInterceptor에서 세션 속성에 저장한 userId를 꺼내
     * userSessions 맵에 해당 세션을 등록합니다.
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 핸드셰이크 인터셉터가 저장한 userId를 세션 속성에서 가져옵니다.
        Long userId = (Long) session.getAttributes().get("userId");

        if (userId == null) {
            log.warn("[WS] userId 없는 세션 감지, 즉시 종료합니다. sessionId={}", session.getId());
            closeQuietly(session);
            return;
        }

        // 해당 userId의 세션 집합에 현재 세션을 추가합니다.
        userSessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
        log.info("[WS] 연결됨. userId={}, sessionId={}, 현재 총 세션 수={}",
                userId, session.getId(), userSessions.get(userId).size());
    }

    /**
     * 클라이언트 연결이 종료되었을 때 호출됩니다 (정상 종료 또는 네트워크 끊김 모두 해당).
     *
     * userSessions 맵에서 해당 세션을 제거하고,
     * 해당 userId의 세션이 모두 없어지면 맵 항목 자체를 삭제합니다.
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) return;

        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(session);
            // 세션이 모두 사라지면 맵에서 키도 제거해 메모리 누수를 방지합니다.
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
            }
        }
        log.info("[WS] 연결 종료. userId={}, sessionId={}, 상태={}",
                userId, session.getId(), status);
    }

    /**
     * 클라이언트로부터 메시지를 수신했을 때 호출됩니다.
     *
     * 현재 서버는 클라이언트에서 메시지를 수신할 필요가 없습니다.
     * (단방향 Push 구조 — 서버 → 클라이언트)
     * 향후 ping/pong이나 읽음 처리 요청을 여기서 처리할 수 있습니다.
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 현재는 클라이언트에서 오는 메시지를 처리하지 않습니다.
        log.debug("[WS] 클라이언트 메시지 수신 (무시됨): {}", message.getPayload());
    }

    /**
     * 특정 사용자의 모든 활성 WebSocket 세션에 메시지를 전송합니다.
     *
     * NotificationService.sendNotification()에서 DB 저장 후 이 메서드를 호출합니다.
     * 전송 실패한 세션은 세션 목록에서 자동으로 제거합니다.
     *
     * @param userId  수신 대상 사용자 ID
     * @param payload 전송할 JSON 문자열 (예: {"type":"NEW_NOTIFICATION"})
     */
    public void pushToUser(Long userId, String payload) {
        Set<WebSocketSession> sessions = userSessions.get(userId);

        // 해당 사용자의 활성 세션이 없으면 조용히 종료합니다 (오류 아님).
        if (sessions == null || sessions.isEmpty()) {
            log.debug("[WS] userId={}의 활성 세션 없음, 푸시 건너뜀.", userId);
            return;
        }

        TextMessage message = new TextMessage(payload);

        // 해당 사용자의 모든 세션(여러 탭 포함)에 메시지를 전송합니다.
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(message);
                    log.debug("[WS] 푸시 성공. userId={}, sessionId={}", userId, session.getId());
                } catch (IOException e) {
                    // 전송 실패 시 해당 세션을 제거하고 계속 진행합니다.
                    log.warn("[WS] 메시지 전송 실패. sessionId={}: {}", session.getId(), e.getMessage());
                    sessions.remove(session);
                }
            } else {
                // 이미 닫힌 세션은 목록에서 제거합니다.
                sessions.remove(session);
            }
        }
    }

    /**
     * 예외 없이 세션을 닫는 헬퍼 메서드입니다.
     * 인증 실패 등으로 연결을 강제 종료할 때 사용합니다.
     */
    private void closeQuietly(WebSocketSession session) {
        try {
            session.close(CloseStatus.POLICY_VIOLATION);
        } catch (IOException e) {
            log.warn("[WS] 세션 종료 중 오류: {}", e.getMessage());
        }
    }
}
