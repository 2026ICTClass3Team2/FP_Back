package com.example.demo.domain.user.controller;

import com.example.demo.domain.user.dto.UserJoinDTO;
import com.example.demo.domain.user.service.UserService;
import com.example.demo.global.exception.CustomJWTException;
import com.example.demo.global.jwt.JWTUtil;
import com.example.demo.global.redis.RedisService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/member")
public class UserController {
    private final UserService userService;
    private final JWTUtil jwtUtil;
    private final RedisService redisService;

    // 회원 가입
    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> join(@RequestBody UserJoinDTO userJoinDTO) {
        log.info("join: {}", userJoinDTO);
        userService.join(userJoinDTO);

        return ResponseEntity.ok(Map.of("result", "success"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(HttpServletRequest request, HttpServletResponse response) {
        // 1. 요청에서 Refresh Token 추출 (Cookie)
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("refreshToken")) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "NO_REFRESH_TOKEN", "message", "Refresh token is missing"));
        }

        try {
            // 2. Refresh Token 검증
            Claims claims = jwtUtil.validateToken(refreshToken);
            String email = claims.get("email", String.class);

            // 3. Redis에 저장된 Refresh Token과 비교 (선택 사항이지만 보안상 강력히 권장)
            String savedRefreshToken = redisService.getRefreshToken(email);
            if (savedRefreshToken == null || !savedRefreshToken.equals(refreshToken)) {
                throw new CustomJWTException("INVALID_REFRESH_TOKEN");
            }

            // 4. 새로운 Access Token 생성
            Map<String, Object> newClaims = new HashMap<>();
            newClaims.put("email", email);
            if (claims.get("roleName") != null) {
                newClaims.put("roleName", claims.get("roleName"));
            }

            String newAccessToken = jwtUtil.generateToken(newClaims, 30); // 30분 재발급

            // 5. Refresh Token의 남은 기간이 1일 미만이면 Refresh Token도 갱신 (RTR 기법)
            long expTime = claims.getExpiration().getTime();
            long now = System.currentTimeMillis();
            if (expTime - now < 1000 * 60 * 60 * 24) { // 1일 미만
                String newRefreshToken = jwtUtil.generateToken(newClaims, 60 * 24 * 7); // 7일 재발급
                
                // Redis 업데이트
                redisService.saveRefreshToken(email, newRefreshToken, 7);
                
                // 쿠키 갱신
                Cookie newRefreshTokenCookie = new Cookie("refreshToken", newRefreshToken);
                newRefreshTokenCookie.setHttpOnly(true);
                newRefreshTokenCookie.setSecure(true);
                newRefreshTokenCookie.setPath("/");
                newRefreshTokenCookie.setMaxAge(7 * 24 * 60 * 60);
                response.addCookie(newRefreshTokenCookie);
            }

            // 6. 새로운 Access Token 반환
            return ResponseEntity.ok(Map.of("accessToken", newAccessToken));

        } catch (CustomJWTException e) {
            log.error("Refresh token validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "EXPIRED_REFRESH_TOKEN", "message", "Refresh token is invalid or expired. Please login again."));
        }
    }
}
