package com.example.demo.global.redis;

import com.example.demo.global.jwt.entity.BlacklistedToken;
import com.example.demo.global.jwt.repository.BlacklistedTokenRepository;
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
    private final BlacklistedTokenRepository blacklistedTokenRepository;

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

    // Access Token을 블랙리스트에 추가 (Redis + DB 하이브리드)
    public void setBlackList(String accessToken, long expirationMillis) {
        // 1. Redis에 저장 (빠른 조회)
        redisTemplate.opsForValue().set(
                "BL:" + accessToken,
                "logout",
                Duration.ofMillis(expirationMillis)
        );

        // 2. 데이터베이스에 저장 (영구적 백업)
        try {
            long expirationTimestamp = System.currentTimeMillis() + expirationMillis;
            BlacklistedToken token = BlacklistedToken.builder()
                    .token(accessToken)
                    .expirationTime(expirationTimestamp)
                    .build();
            blacklistedTokenRepository.save(token);
        } catch (Exception e) {
            log.error("Failed to save blacklisted token to database: {}", e.getMessage());
        }
    }

    // Access Token이 블랙리스트에 있는지 확인 (Redis 조회 -> DB 조회)
    public boolean isBlackList(String accessToken) {
        // 1. Redis에서 먼저 확인 (메모리라 속도가 빠름)
        Boolean isBlacklistedInRedis = redisTemplate.hasKey("BL:" + accessToken);
        if (Boolean.TRUE.equals(isBlacklistedInRedis)) {
            return true;
        }

        // 2. Redis에 없거나 만료되어 지워졌을 경우를 대비해 DB에서 한 번 더 확인 (Fallback)
        return blacklistedTokenRepository.existsByToken(accessToken);
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
}
