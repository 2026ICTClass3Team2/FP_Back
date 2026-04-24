package com.example.demo.global.config;

import com.example.demo.domain.user.repository.UserRepository;
import com.example.demo.global.jwt.JWTUtil;
import com.example.demo.global.websocket.JwtHandshakeInterceptor;
import com.example.demo.global.websocket.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 설정 클래스입니다.
 *
 * 역할:
 *  - /ws/notifications 엔드포인트에 WebSocket 핸들러를 등록합니다.
 *  - JWT 핸드셰이크 인터셉터를 연결해 인증되지 않은 연결을 차단합니다.
 *  - setAllowedOriginPatterns("*")으로 개발/운영 환경 모두에서 CORS를 허용합니다.
 *    (운영 환경에서는 실제 프론트엔드 도메인으로 좁히는 것을 권장합니다.)
 *
 * 엔드포인트 URL: ws://localhost:8090/ws/notifications?token=<JWT>
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    // 실제 WebSocket 연결 및 세션 관리를 담당하는 핸들러
    private final NotificationWebSocketHandler notificationWebSocketHandler;

    // JWT 토큰 검증 유틸리티 (핸드셰이크 인터셉터에 주입)
    private final JWTUtil jwtUtil;

    // 토큰에서 추출한 이메일로 사용자를 조회하기 위한 레포지토리 (핸드셰이크 인터셉터에 주입)
    private final UserRepository userRepository;

    /**
     * WebSocket 핸들러와 엔드포인트를 등록합니다.
     *
     * 흐름:
     *  1. 클라이언트가 ws://.../ws/notifications?token=... 으로 연결 요청
     *  2. JwtHandshakeInterceptor.beforeHandshake()가 토큰을 검증
     *  3. 검증 성공 시 NotificationWebSocketHandler.afterConnectionEstablished() 호출
     *  4. 이후 NotificationService에서 pushToUser()를 호출해 실시간 알림 전송
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
            // 핸들러와 엔드포인트 경로를 등록합니다.
            .addHandler(notificationWebSocketHandler, "/ws/notifications")
            // JWT 인증 인터셉터를 추가합니다.
            // new로 직접 생성하는 이유: HandshakeInterceptor는 @Bean이 아니어도
            // WebSocketConfigurer의 registry를 통해 등록 가능하기 때문입니다.
            .addInterceptors(new JwtHandshakeInterceptor(jwtUtil, userRepository))
            // CORS 허용 — 운영 시 실제 프론트엔드 도메인으로 교체하세요.
            .setAllowedOriginPatterns("*");
    }
}
