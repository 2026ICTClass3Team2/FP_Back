package com.example.demo.domain.user.controller;

import com.example.demo.domain.user.dto.EmailAuthRequestDTO;
import com.example.demo.domain.user.dto.EmailVerifyRequestDTO;
import com.example.demo.domain.user.dto.MemberDTO;
import com.example.demo.domain.user.dto.UserJoinDTO;
import com.example.demo.domain.user.dto.UserSummaryDTO;
import com.example.demo.domain.user.service.MailService;
import com.example.demo.domain.user.service.UserService;
import com.example.demo.global.exception.CustomJWTException;
import com.example.demo.global.jwt.JWTUtil;
import com.example.demo.global.redis.RedisService;
import io.jsonwebtoken.Claims;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/member")
public class UserController {
    private final UserService userService;
    private final MailService mailService;
    private final JWTUtil jwtUtil;
    private final RedisService redisService;

    // 회원 가입
    @PostMapping("/signup")
    public ResponseEntity<?> join(@Valid @RequestBody UserJoinDTO userJoinDTO, BindingResult bindingResult) {
        log.info("join: {}", userJoinDTO);

        // 1. 유효성 검사 에러 처리
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            for (FieldError error : bindingResult.getFieldErrors()) {
                errors.put(error.getField(), error.getDefaultMessage());
            }
            return ResponseEntity.badRequest().body(errors);
        }

        try {
            userService.join(userJoinDTO);
            return ResponseEntity.ok(Map.of("result", "success"));
        } catch (IllegalArgumentException e) {
            log.error("Signup failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 아이디 중복 확인
    @GetMapping("/check-username")
    public ResponseEntity<Map<String, Boolean>> checkUsername(@RequestParam String username) {
        boolean isDuplicate = userService.checkUsername(username);
        return ResponseEntity.ok(Map.of("isDuplicate", isDuplicate));
    }

    // 이메일 중복 확인
    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Boolean>> checkEmail(@RequestParam String email) {
        boolean isDuplicate = userService.checkEmail(email);
        return ResponseEntity.ok(Map.of("isDuplicate", isDuplicate));
    }

    // 이메일 인증 번호 전송
    @PostMapping("/email/send")
    public ResponseEntity<?> sendAuthEmail(@Valid @RequestBody EmailAuthRequestDTO requestDTO, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            FieldError fieldError = bindingResult.getFieldError();
            String errorMessage = (fieldError != null && fieldError.getDefaultMessage() != null) ? fieldError.getDefaultMessage() : "유효하지 않은 요청입니다.";
            return ResponseEntity.badRequest().body(Map.of("error", errorMessage));
        }

        try {
            // 이메일 중복 확인 먼저
            if (userService.checkEmail(requestDTO.getEmail())) {
                return ResponseEntity.badRequest().body(Map.of("error", "이미 가입된 이메일입니다."));
            }

            mailService.sendSimpleMessage(requestDTO.getEmail());
            return ResponseEntity.ok(Map.of("message", "인증 번호가 전송되었습니다."));
        } catch (MessagingException | IllegalArgumentException e) {
            log.error("Email sending failed: ", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "메일 발송에 실패했습니다."));
        }
    }

    // 이메일 인증 번호 검증
    @PostMapping("/email/verify")
    public ResponseEntity<?> verifyAuthCode(@Valid @RequestBody EmailVerifyRequestDTO requestDTO, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            FieldError fieldError = bindingResult.getFieldError();
            String errorMessage = (fieldError != null && fieldError.getDefaultMessage() != null) ? fieldError.getDefaultMessage() : "유효하지 않은 요청입니다.";
            return ResponseEntity.badRequest().body(Map.of("error", errorMessage));
        }

        boolean isVerified = mailService.verifyAuthCode(requestDTO.getEmail(), requestDTO.getCode());
        
        if (isVerified) {
            return ResponseEntity.ok(Map.of("message", "인증에 성공했습니다.", "verified", true));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "인증 번호가 일치하지 않거나 만료되었습니다.", "verified", false));
        }
    }

    // OAuth 신규 가입 후 username 설정 (JWT 필요)
    @PatchMapping("/oauth/setup-username")
    public ResponseEntity<?> setupOAuthUsername(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal MemberDTO memberDTO) {
        String username = body.get("username");
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "아이디를 입력해주세요."));
        }
        try {
            userService.setupOAuthUsername(memberDTO.getEmail(), username);
            return ResponseEntity.ok(Map.of("result", "success"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 비밀번호 찾기 — 재설정 메일 발송
    @PostMapping("/password/forgot")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "이메일을 입력해주세요."));
        }
        try {
            userService.sendPasswordResetEmail(email);
            return ResponseEntity.ok(Map.of("message", "비밀번호 재설정 메일이 발송되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (MessagingException e) {
            log.error("Password reset email failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "메일 발송에 실패했습니다."));
        }
    }

    // 비밀번호 재설정 토큰 유효성 확인
    @GetMapping("/password/verify-token")
    public ResponseEntity<?> verifyPasswordResetToken(@RequestParam String token) {
        boolean valid = userService.verifyPasswordResetToken(token);
        if (valid) {
            return ResponseEntity.ok(Map.of("valid", true));
        }
        return ResponseEntity.badRequest().body(Map.of("valid", false, "error", "유효하지 않거나 만료된 링크입니다."));
    }

    // 비밀번호 재설정
    @PostMapping("/password/reset")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String newPassword = body.get("newPassword");
        if (token == null || token.isBlank() || newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "요청이 유효하지 않습니다."));
        }
        if (newPassword.length() < 8 || newPassword.length() > 32 || newPassword.contains(" ")) {
            return ResponseEntity.badRequest().body(Map.of("error", "비밀번호는 공백 없이 8~32자여야 합니다."));
        }
        try {
            userService.resetPassword(token, newPassword);
            return ResponseEntity.ok(Map.of("message", "비밀번호가 성공적으로 변경되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 유저 검색 (멘션용)
    @GetMapping("/search")
    public ResponseEntity<List<UserSummaryDTO>> searchUsers(@RequestParam String query) {
        return ResponseEntity.ok(userService.searchUsersForMention(query));
    }

    // Refresh Token으로 Access Token 재발급
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(HttpServletRequest request, HttpServletResponse response) {
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
            Claims claims = jwtUtil.validateToken(refreshToken);
            String email = claims.get("email", String.class);
            String savedRefreshToken = redisService.getRefreshToken(email);
            if (savedRefreshToken == null || !savedRefreshToken.equals(refreshToken)) {
                throw new CustomJWTException("INVALID_REFRESH_TOKEN");
            }
            Map<String, Object> newClaims = new HashMap<>();
            newClaims.put("email", email);
            if (claims.get("roleName") != null) {
                newClaims.put("roleName", claims.get("roleName"));
            }
            String newAccessToken = jwtUtil.generateToken(newClaims, 30);

            // RTR: Access Token 갱신 시 항상 Refresh Token도 교체
            String newRefreshToken = jwtUtil.generateToken(newClaims, 60 * 24 * 7);
            redisService.saveRefreshToken(email, newRefreshToken, 7);
            Cookie newRefreshTokenCookie = new Cookie("refreshToken", newRefreshToken);
            newRefreshTokenCookie.setHttpOnly(true);
            newRefreshTokenCookie.setSecure(true);
            newRefreshTokenCookie.setPath("/");
            newRefreshTokenCookie.setMaxAge(7 * 24 * 60 * 60);
            response.addCookie(newRefreshTokenCookie);

            return ResponseEntity.ok(Map.of("accessToken", newAccessToken));

        } catch (CustomJWTException e) {
            log.error("Refresh token validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "EXPIRED_REFRESH_TOKEN", "message", "Refresh token is invalid or expired. Please login again."));
        }
    }
}
