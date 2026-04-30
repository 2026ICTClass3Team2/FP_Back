package com.example.demo.global.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {
    private final StringRedisTemplate redisTemplate;

    public void saveRefreshToken(String email, String refreshToken, long durationDays) {
        redisTemplate.opsForValue().set(
                "RT:" + email,
                refreshToken,
                Duration.ofDays(durationDays)
        );
    }

    public String getRefreshToken(String email) {
        return redisTemplate.opsForValue().get("RT:" + email);
    }

    public void deleteRefreshToken(String email) {
        redisTemplate.delete("RT:" + email);
    }

    // 이메일 인증번호 저장
    public void saveAuthCode(String email, String code, long durationMillis) {
        redisTemplate.opsForValue().set(
                "AUTH:" + email,
                code,
                Duration.ofMillis(durationMillis)
        );
    }

    // 이메일 인증번호 조회
    public String getAuthCode(String email) {
        return redisTemplate.opsForValue().get("AUTH:" + email);
    }

    // 이메일 인증번호 삭제
    public void deleteAuthCode(String email) {
        redisTemplate.delete("AUTH:" + email);
    }

    // 비밀번호 재설정 토큰 저장 (30분 유효)
    public void savePasswordResetToken(String token, String email) {
        redisTemplate.opsForValue().set(
                "PWD_RESET:" + token,
                email,
                Duration.ofMinutes(30)
        );
    }

    public String getPasswordResetEmail(String token) {
        return redisTemplate.opsForValue().get("PWD_RESET:" + token);
    }

    public void deletePasswordResetToken(String token) {
        redisTemplate.delete("PWD_RESET:" + token);
    }

    // Access Token 블랙리스트 추가
    public void setBlackList(String accessToken, long durationMillis) {
        redisTemplate.opsForValue().set(
                "BKLST:" + accessToken,
                "logout",
                Duration.ofMillis(durationMillis)
        );
    }

    public boolean isBlackListed(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("BKLST:" + accessToken));
    }
}
