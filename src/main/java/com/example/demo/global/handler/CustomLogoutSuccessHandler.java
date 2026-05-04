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

        // 1. 현재 인증된 사용자 정보 가져오기 (SecurityContext에서)
        if (authentication != null && authentication.getName() != null) {
            username = authentication.getName();
        }

        // 2. Access Token 블랙리스트 등록 (잔여 유효기간만큼 TTL)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);
            try {
                Claims claims = jwtUtil.validateToken(accessToken);
                long remaining = claims.getExpiration().getTime() - System.currentTimeMillis();
                if (remaining > 0) {
                    redisService.setBlackList(accessToken, remaining);
                    log.info("Access Token blacklisted for user: {}", username);
                }
            } catch (Exception e) {
                log.warn("Failed to blacklist access token: {}", e.getMessage());
            }
        }

        // 3. Redis에서 Refresh Token 삭제
        if (username != null) {
            redisService.deleteRefreshToken(username);
            log.info("Refresh Token deleted from Redis for user: {}", username);
        }

        // 4. 브라우저 쿠키에서 Refresh Token 삭제
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setMaxAge(0); // 0으로 설정하여 즉시 만료
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setAttribute("SameSite", "None");
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
