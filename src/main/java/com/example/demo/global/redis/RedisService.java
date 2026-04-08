package com.example.demo.global.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

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

    // Access Token을 블랙리스트에 추가하는 메서드 (폐기 전략)
    public void setBlackList(String accessToken, long expirationMillis) {
        redisTemplate.opsForValue().set(
                "BL:" + accessToken,
                "logout", // 값은 의미가 없으므로 식별하기 쉬운 문자열 사용
                Duration.ofMillis(expirationMillis)
        );
    }

    // Access Token이 블랙리스트에 있는지 확인하는 메서드
    public boolean isBlackList(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("BL:" + accessToken));
    }
}
