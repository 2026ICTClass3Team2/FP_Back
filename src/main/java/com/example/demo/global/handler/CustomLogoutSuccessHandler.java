package com.example.demo.global.handler;

import com.example.demo.global.redis.RedisService;
import com.google.gson.Gson;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {
    private final RedisService redisService;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        String username = null;

        // 1. 현재 인증된 사용자 정보 가져오기
        if (authentication != null && authentication.getName() != null) {
            username = authentication.getName();
        }

        // 2. Redis에서 Refresh Token 삭제
        if (username != null) {
            redisService.deleteRefreshToken(username);
        }

        // 3. 쿠키에서 Refresh Token 삭제
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
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
