package com.example.demo.domain.user.scheduler;

import com.example.demo.domain.user.repository.InterestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterestDecayScheduler {

    private static final double DECAY_FACTOR = 0.9;
    // 이 값 미만이면 관심도가 사실상 0에 수렴 → 삭제해 추천 노이즈 방지
    private static final double MIN_SCORE_THRESHOLD = 0.05;

    private final InterestRepository interestRepository;

    // 매일 새벽 4시 실행 (UserStatusScheduler 3시와 겹치지 않도록)
    @Scheduled(cron = "0 0 4 * * ?")
    @Transactional
    public void applyDailyDecay() {
        LocalDateTime now = LocalDateTime.now();

        int updated = interestRepository.decayAllNonProfileInterests(DECAY_FACTOR, now);
        int deleted = interestRepository.deleteByWeightScoreLessThanAndIsProfileTagFalse(MIN_SCORE_THRESHOLD);

        log.info("[InterestDecay] decayed={}, pruned={}", updated, deleted);
    }
}
