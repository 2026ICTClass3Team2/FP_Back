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

    // Access Token 블랙리스트 추가
    public void setBlackList(String accessToken, long durationMillis) {
        redisTemplate.opsForValue().set(
                "BKLST:" + accessToken,
                "logout", // 값은 중요하지 않음, 키의 존재 여부가 중요
                Duration.ofMillis(durationMillis)
        );
    }
}
