package com.example.demo.domain.algorithm.service;

public interface UserInterestService {

    void onView(Long userId, Long postId);

    void onLike(Long userId, Long postId);

    void onUnlike(Long userId, Long postId);

    void onDislike(Long userId, Long postId);

    void onUndislike(Long userId, Long postId);

    /** 좋아요 상태에서 싫어요로 전환 — 단일 트랜잭션에서 합산 delta 적용 */
    void onSwitchLikeToDislike(Long userId, Long postId);

    /** 싫어요 상태에서 좋아요로 전환 — 단일 트랜잭션에서 합산 delta 적용 */
    void onSwitchDislikeToLike(Long userId, Long postId);

    void onComment(Long userId, Long postId);

    void onBookmark(Long userId, Long postId);

    void onUnbookmark(Long userId, Long postId);

    void onShare(Long userId, Long postId);

    void onNotInterested(Long userId, Long postId);

    void onPostWrite(Long userId, Long postId);

    void onChannelSubscribe(Long userId, Long channelId);

    void onChannelUnsubscribe(Long userId, Long channelId);
}
