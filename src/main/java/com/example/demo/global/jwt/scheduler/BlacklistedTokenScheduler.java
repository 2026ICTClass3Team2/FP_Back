package com.example.demo.global.jwt.scheduler;

import com.example.demo.global.jwt.repository.BlacklistedTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class BlacklistedTokenScheduler {

    private final BlacklistedTokenRepository blacklistedTokenRepository;

    // 매일 자정에 실행 (cron 표현식: 초 분 시 일 월 요일)
    // 혹은 토큰의 최대 생명 주기에 맞춰서 (예: 1시간 마다)
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanUpExpiredBlacklistedTokens() {
        long currentTime = System.currentTimeMillis();
        
        try {
            blacklistedTokenRepository.deleteExpiredTokens(currentTime);
            log.info("Expired blacklisted tokens have been cleaned up from DB.");
        } catch (Exception e) {
            log.error("Failed to clean up expired blacklisted tokens: {}", e.getMessage());
        }
    }
}
