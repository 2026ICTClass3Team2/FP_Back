package com.example.demo.global.config;

import com.example.demo.domain.user.repository.UserRepository;
import com.example.demo.global.jwt.JWTUtil;
import com.example.demo.global.websocket.ChatWebSocketHandler;
import com.example.demo.global.websocket.JwtHandshakeInterceptor;
import com.example.demo.global.websocket.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    // 실제 WebSocket 연결 및 세션 관리를 담당하는 핸들러 (알림)
    private final NotificationWebSocketHandler notificationWebSocketHandler;

    // 1:1 채팅을 담당하는 핸들러
    private final ChatWebSocketHandler chatWebSocketHandler;

    // JWT 토큰 검증 유틸리티 (핸드셰이크 인터셉터에 주입)
    private final JWTUtil jwtUtil;

    // 토큰에서 추출한 이메일로 사용자를 조회하기 위한 레포지토리 (핸드셰이크 인터셉터에 주입)
    private final UserRepository userRepository;

    /**
     * WebSocket 핸들러와 엔드포인트를 등록합니다.
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 알림 전용 엔드포인트
        registry
            .addHandler(notificationWebSocketHandler, "/ws/notifications")
            .addInterceptors(new JwtHandshakeInterceptor(jwtUtil, userRepository))
            .setAllowedOriginPatterns("*");

        // 1:1 채팅 전용 엔드포인트
        registry
            .addHandler(chatWebSocketHandler, "/ws/chat")
            .addInterceptors(new JwtHandshakeInterceptor(jwtUtil, userRepository))
            .setAllowedOriginPatterns("*");
    }
}
