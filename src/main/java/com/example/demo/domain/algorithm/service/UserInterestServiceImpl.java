package com.example.demo.domain.algorithm.service;

import com.example.demo.domain.channel.repository.ChannelTagRepository;
import com.example.demo.domain.content.entity.ContentTag;
import com.example.demo.domain.content.entity.Tag;
import com.example.demo.domain.content.repository.ContentTagRepository;
import com.example.demo.domain.interaction.entity.ViewHistory;
import com.example.demo.domain.interaction.repository.ViewHistoryRepository;
import com.example.demo.domain.user.entity.Interest;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.InterestRepository;
import com.example.demo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserInterestServiceImpl implements UserInterestService {

    private static final double DAILY_DECAY = 0.99;

    private static final double DELTA_VIEW = 0.3;
    private static final double DELTA_LIKE = 3.0;
    private static final double DELTA_DISLIKE = 2.0;
    private static final double DELTA_COMMENT = 2.0;
    private static final double DELTA_BOOKMARK = 4.0;
    private static final double DELTA_SHARE = 2.0;
    private static final double DELTA_NOT_INTERESTED = 5.0;
    private static final double DELTA_POST_WRITE = 5.0;
    private static final double DELTA_CHANNEL_SUBSCRIBE = 3.0;

    private static final Duration DEDUP_VIEW    = Duration.ofHours(1);
    private static final Duration DEDUP_COMMENT = Duration.ofHours(1);
    private static final Duration DEDUP_SHARE   = Duration.ofHours(1);

    // 스키마 변경 없이 comment/share 중복 방지 — (type:userId:postId) → 마지막 발생 시각
    private final ConcurrentHashMap<String, LocalDateTime> behaviorCache = new ConcurrentHashMap<>();

    private final ContentTagRepository contentTagRepository;
    private final ChannelTagRepository channelTagRepository;
    private final InterestRepository interestRepository;
    private final UserRepository userRepository;
    private final ViewHistoryRepository viewHistoryRepository;

    @Async
    @Override
    @Transactional
    public void onView(Long userId, Long postId) {
        LocalDateTime threshold = LocalDateTime.now().minus(DEDUP_VIEW);
        if (viewHistoryRepository.existsByUser_IdAndPostIdAndViewedAtAfter(userId, postId, threshold)) return;

        User user = userRepository.getReferenceById(userId);
        viewHistoryRepository.save(ViewHistory.builder()
                .user(user)
                .postId(postId)
                .build());
        applyDeltaToPostTags(userId, postId, +DELTA_VIEW);
    }

    @Async
    @Override
    @Transactional
    public void onLike(Long userId, Long postId) {
        applyDeltaToPostTags(userId, postId, +DELTA_LIKE);
    }

    @Async
    @Override
    @Transactional
    public void onUnlike(Long userId, Long postId) {
        applyDeltaToPostTags(userId, postId, -DELTA_LIKE);
    }

    @Async
    @Override
    @Transactional
    public void onDislike(Long userId, Long postId) {
        applyDeltaToPostTags(userId, postId, -DELTA_DISLIKE);
    }

    @Async
    @Override
    @Transactional
    public void onUndislike(Long userId, Long postId) {
        applyDeltaToPostTags(userId, postId, +DELTA_DISLIKE);
    }

    @Async
    @Override
    @Transactional
    public void onSwitchLikeToDislike(Long userId, Long postId) {
        applyDeltaToPostTags(userId, postId, -DELTA_LIKE - DELTA_DISLIKE);
    }

    @Async
    @Override
    @Transactional
    public void onSwitchDislikeToLike(Long userId, Long postId) {
        applyDeltaToPostTags(userId, postId, +DELTA_LIKE + DELTA_DISLIKE);
    }

    @Async
    @Override
    @Transactional
    public void onComment(Long userId, Long postId) {
        if (isDuplicateCached(userId, postId, "COMMENT", DEDUP_COMMENT)) return;
        applyDeltaToPostTags(userId, postId, +DELTA_COMMENT);
    }

    @Async
    @Override
    @Transactional
    public void onBookmark(Long userId, Long postId) {
        applyDeltaToPostTags(userId, postId, +DELTA_BOOKMARK);
    }

    @Async
    @Override
    @Transactional
    public void onUnbookmark(Long userId, Long postId) {
        applyDeltaToPostTags(userId, postId, -DELTA_BOOKMARK);
    }

    @Async
    @Override
    @Transactional
    public void onShare(Long userId, Long postId) {
        if (isDuplicateCached(userId, postId, "SHARE", DEDUP_SHARE)) return;
        applyDeltaToPostTags(userId, postId, +DELTA_SHARE);
    }

    @Async
    @Override
    @Transactional
    public void onNotInterested(Long userId, Long postId) {
        applyDeltaToPostTags(userId, postId, -DELTA_NOT_INTERESTED);
    }

    @Async
    @Override
    @Transactional
    public void onPostWrite(Long userId, Long postId) {
        applyDeltaToPostTags(userId, postId, +DELTA_POST_WRITE);
    }

    @Async
    @Override
    @Transactional
    public void onChannelSubscribe(Long userId, Long channelId) {
        applyDeltaToChannelTags(userId, channelId, +DELTA_CHANNEL_SUBSCRIBE);
    }

    @Async
    @Override
    @Transactional
    public void onChannelUnsubscribe(Long userId, Long channelId) {
        applyDeltaToChannelTags(userId, channelId, -DELTA_CHANNEL_SUBSCRIBE);
    }

    /**
     * 인메모리 캐시로 중복 행동 방지.
     * 동일 (type, userId, postId) 조합이 window 내에 이미 존재하면 true(스킵).
     * 최초 또는 window 만료 시 캐시를 갱신하고 false 반환.
     */
    private boolean isDuplicateCached(Long userId, Long postId, String type, Duration window) {
        String key = type + ":" + userId + ":" + postId;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last = behaviorCache.get(key);
        if (last != null && Duration.between(last, now).compareTo(window) < 0) {
            return true;
        }
        behaviorCache.put(key, now);
        return false;
    }

    private void applyDeltaToPostTags(Long userId, Long postId, double delta) {
        List<ContentTag> tags = contentTagRepository.findByPost_Id(postId);
        if (tags.isEmpty()) return;
        User user = userRepository.getReferenceById(userId);
        for (ContentTag ct : tags) {
            updateInterest(user, ct.getTag(), delta);
        }
    }

    private void applyDeltaToChannelTags(Long userId, Long channelId, double delta) {
        channelTagRepository.findByChannel_Id(channelId).forEach(ct -> {
            User user = userRepository.getReferenceById(userId);
            updateInterest(user, ct.getTag(), delta);
        });
    }

    private void updateInterest(User user, Tag tag, double delta) {
        Optional<Interest> existing = interestRepository.findByUserAndTag(user, tag);
        LocalDateTime now = LocalDateTime.now();

        if (existing.isPresent()) {
            Interest interest = existing.get();
            long days = ChronoUnit.DAYS.between(interest.getLastInteractionAt(), now);
            double decayed = interest.getWeightScore() * Math.pow(DAILY_DECAY, days);
            double newScore = Math.max(0.0, decayed + delta);
            interest.updateScore(newScore, now);
        } else {
            if (delta > 0) {
                interestRepository.save(Interest.builder()
                        .user(user)
                        .tag(tag)
                        .weightScore(delta)   // 기본값 없이 행동 델타로 시작
                        .isProfileTag(false)
                        .lastInteractionAt(now)
                        .build());
            }
        }
    }

}
