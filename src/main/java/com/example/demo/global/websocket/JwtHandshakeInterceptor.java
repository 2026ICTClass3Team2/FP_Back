package com.example.demo.global.websocket;

import com.example.demo.global.jwt.JWTUtil;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

/**
 * WebSocket 핸드셰이크(HTTP → WS 업그레이드) 전에 실행되는 인터셉터입니다.
 *
 * 역할:
 *  - URL 쿼리 파라미터에서 JWT 토큰을 추출합니다.
 *  - JWTUtil을 사용해 토큰을 검증하고 이메일 클레임을 가져옵니다.
 *  - 이메일로 사용자를 조회해 userId를 WebSocket 세션 속성에 저장합니다.
 *  - 토큰이 없거나 유효하지 않으면 false를 반환해 연결을 거부합니다.
 *
 * 브라우저는 WebSocket 핸드셰이크 시 커스텀 HTTP 헤더를 설정할 수 없기 때문에
 * JWT를 URL 쿼리 파라미터(?token=...)로 전달하는 방식을 사용합니다.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;

    /**
     * 핸드셰이크 전 처리 — 토큰 검증 및 userId를 세션 속성에 저장합니다.
     *
     * @param attributes WebSocket 세션에 저장할 속성 맵
     * @return true면 업그레이드 허용, false면 거부(403)
     */
    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        // 1. URL 쿼리 스트링에서 token 파라미터를 추출합니다.
        URI uri = request.getURI();
        String query = uri.getQuery(); // 예: "token=eyJ..."

        if (query == null || !query.contains("token=")) {
            log.warn("[WS 핸드셰이크] 토큰이 없어 연결을 거부합니다. URI: {}", uri);
            return false; // 토큰 없음 → 연결 거부
        }

        // 2. 쿼리 문자열을 파싱해 token 값만 추출합니다.
        String token = null;
        for (String param : query.split("&")) {
            if (param.startsWith("token=")) {
                token = param.substring("token=".length());
                break;
            }
        }

        if (token == null || token.isBlank()) {
            log.warn("[WS 핸드셰이크] token 파라미터가 비어 있습니다.");
            return false;
        }

        try {
            // 3. JWT 토큰을 검증하고 클레임(이메일)을 추출합니다.
            Claims claims = jwtUtil.validateToken(token);
            String email = claims.getSubject();

            // 4. 이메일로 사용자를 조회해 userId를 가져옵니다.
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                log.warn("[WS 핸드셰이크] 이메일에 해당하는 사용자를 찾을 수 없습니다: {}", email);
                return false;
            }

            Long userId = userOpt.get().getId();

            // 5. userId를 세션 속성에 저장합니다.
            //    이 값은 afterConnectionEstablished()에서 세션 등록에 사용됩니다.
            attributes.put("userId", userId);
            log.info("[WS 핸드셰이크] 인증 성공. userId={}", userId);
            return true; // 연결 허용

        } catch (Exception e) {
            // 토큰이 만료되었거나 형식이 잘못된 경우 연결을 거부합니다.
            log.warn("[WS 핸드셰이크] 유효하지 않은 JWT 토큰: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 핸드셰이크 완료 후 처리 — 별도 작업 없음.
     */
    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // 완료 후 별도 처리 없음
    }
}
