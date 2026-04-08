package com.example.demo.global.handler;

import com.example.demo.global.jwt.JWTUtil;
import com.example.demo.global.redis.RedisService;
import com.google.gson.Gson;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {
    private final RedisService redisService;
    private final JWTUtil jwtUtil;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        String username = null;
        String accessToken = null;

        // 1. 헤더에서 Access Token 추출하여 블랙리스트 처리 (폐기 전략)
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            accessToken = authorizationHeader.substring(7);
            try {
                // Access Token의 남은 유효시간 계산
                Claims claims = jwtUtil.validateToken(accessToken);
                long expirationTime = claims.getExpiration().getTime();
                long now = System.currentTimeMillis();
                long remainTime = expirationTime - now;

                if (remainTime > 0) {
                    // Redis에 블랙리스트로 등록 (토큰 폐기)
                    redisService.setBlackList(accessToken, remainTime);
                    log.info("Access Token added to Blacklist. Remaining time: {} ms", remainTime);
                }
            } catch (Exception e) {
                // 토큰이 이미 만료되었거나 손상된 경우 무시
                log.warn("Invalid or expired Access Token during logout: {}", e.getMessage());
            }
        }

        // 2. 현재 인증된 사용자 정보 가져오기 (SecurityContext에서)
        if (authentication != null && authentication.getName() != null) {
            username = authentication.getName();
        }

        // 3. Redis에서 Refresh Token 삭제 (DB에서 폐기)
        if (username != null) {
            redisService.deleteRefreshToken(username);
            log.info("Refresh Token deleted from Redis for user: {}", username);
        }

        // 4. 브라우저 쿠키에서 Refresh Token 삭제 (클라이언트에서 폐기)
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        response.addCookie(cookie);

        // 5. 응답 전송
        Gson gson = new Gson();
        String jsonStr = gson.toJson(Map.of("message", "Logout successful"));
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);

        PrintWriter pw = response.getWriter();
        pw.println(jsonStr);
        pw.close();
    }
}
