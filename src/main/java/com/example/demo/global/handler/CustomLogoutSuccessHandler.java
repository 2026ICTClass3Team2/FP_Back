package com.example.demo.global.handler;

import com.example.demo.global.redis.RedisService;
import com.google.gson.Gson;
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

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        String username = null;

        // 1. 현재 인증된 사용자 정보 가져오기 (SecurityContext에서)
        if (authentication != null && authentication.getName() != null) {
            username = authentication.getName();
        }

        // 2. Redis에서 Refresh Token 삭제
        if (username != null) {
            redisService.deleteRefreshToken(username);
            log.info("Refresh Token deleted from Redis for user: {}", username);
        }

        // 3. 브라우저 쿠키에서 Refresh Token 삭제 (클라이언트에서 폐기)
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setMaxAge(0); // 0으로 설정하여 즉시 만료
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        
        // 중요: 쿠키의 도메인(domain)이나 SameSite 속성이 프론트엔드 환경과 다를 경우 
        // 브라우저가 쿠키 삭제를 무시할 수 있으므로, 필요하다면 SameSite 속성 추가
        cookie.setAttribute("SameSite", "None"); 

        response.addCookie(cookie);

        // 4. 응답 전송
        Gson gson = new Gson();
        String jsonStr = gson.toJson(Map.of("message", "Logout successful"));
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);

        PrintWriter pw = response.getWriter();
        pw.println(jsonStr);
        pw.close();
    }
}
