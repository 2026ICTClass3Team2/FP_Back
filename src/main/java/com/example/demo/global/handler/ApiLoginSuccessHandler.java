package com.example.demo.global.handler;

import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
import com.example.demo.global.jwt.JWTUtil;
import com.example.demo.global.redis.RedisService;
import com.google.gson.Gson;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ApiLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final JWTUtil jwtUtil;
    private final RedisService redisService;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(@Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull Authentication authentication) throws IOException {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Objects.requireNonNull(userDetails, "UserDetails cannot be null");
        String username = userDetails.getUsername(); // 이메일이나 ID 등

        Map<String, Object> claims = new HashMap<>(Map.of("email", username));

        // 1. Access Token 발급 (30분)
        String accessToken = jwtUtil.generateToken(claims, 30);

        // 2. Refresh Token 발급 (7일)
        String refreshToken = jwtUtil.generateToken(claims, 60 * 24 * 7);

        // 3. Redis에 Refresh Token 저장
        redisService.saveRefreshToken(username, refreshToken, 7);

        // 4. Refresh Token 쿠키에 담기
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true); // HTTPS 환경에서만 전송
        refreshTokenCookie.setPath("/"); // 모든 경로에서 접근 가능
        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7일 (초 단위)
        response.addCookie(refreshTokenCookie);

        // 5. 클라이언트(React) 응답 바디로 Access Token 전송
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("accessToken", accessToken);
        responseData.put("username", username);
        
        User user = userRepository.findByEmail(username).orElse(null);
        if (user != null) {
            responseData.put("userId", user.getId());
            responseData.put("role", user.getRole().name());
        }

        Gson gson = new Gson();
        String jsonStr = gson.toJson(responseData);
        response.setContentType("application/json;charset=utf-8");
        PrintWriter pw = response.getWriter();
        pw.println(jsonStr);
        pw.close();
    }
}
