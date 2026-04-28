package com.example.demo.domain.content.repository;

import com.example.demo.domain.content.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    long countByContentType(String contentType);

    long countByChannel_IdAndContentTypeAndStatus(Long channelId, String contentType, String status);

    // 무한 스크롤용 - Cursor 기반 페이징 (No-Offset)
    @Query("SELECT p FROM Post p " +
           "LEFT JOIN Hidden h ON h.targetId = p.id AND h.targetType = p.contentType AND h.user.id = :currentUserId " +
           "LEFT JOIN Block b ON b.blocked.id = p.author.id AND b.blocker.id = :currentUserId " +
           "WHERE p.id < :lastPostId AND h.id IS NULL AND b.id IS NULL AND " +
           "p.contentType = 'feed' AND p.status = 'active' AND " +
           "(p.channel IS NULL OR p.channel.status = 'active') " +
           "ORDER BY p.id DESC")
    Slice<Post> findPostsByCursor(@Param("lastPostId") Long lastPostId, @Param("currentUserId") Long currentUserId, Pageable pageable);

    // 무한 스크롤용 - 첫 페이지 조회 (lastPostId가 없을 때)
    @Query("SELECT p FROM Post p " +
           "LEFT JOIN Hidden h ON h.targetId = p.id AND h.targetType = p.contentType AND h.user.id = :currentUserId " +
           "LEFT JOIN Block b ON b.blocked.id = p.author.id AND b.blocker.id = :currentUserId " +
           "WHERE h.id IS NULL AND b.id IS NULL AND " +
           "p.contentType = 'feed' AND p.status = 'active' AND " +
           "(p.channel IS NULL OR p.channel.status = 'active') " +
           "ORDER BY p.id DESC")
    Slice<Post> findPostsFirstPage(@Param("currentUserId") Long currentUserId, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
    void increaseViewCount(@Param("postId") Long postId);

    @Query("SELECT p FROM Post p " +
           "LEFT JOIN p.channel ch " +
           "LEFT JOIN Hidden h ON h.targetId = p.id AND h.targetType = p.contentType AND h.user.id = :currentUserId " +
           "LEFT JOIN Block b ON b.blocked.id = p.author.id AND b.blocker.id = :currentUserId " +
           "WHERE p.author.id = :authorId AND p.contentType IN :contentTypes AND p.status IN ('active', 'frozen') AND h.id IS NULL AND b.id IS NULL " +
           "AND (p.channel IS NULL OR ch.status = 'active')")
    Page<Post> findByAuthorIdAndContentTypeIn(@Param("authorId") Long authorId, @Param("contentTypes") List<String> contentTypes, @Param("currentUserId") Long currentUserId, Pageable pageable);

    @Query("SELECT p FROM Post p " +
           "LEFT JOIN Hidden h ON h.targetId = p.id AND h.targetType = p.contentType AND h.user.id = :currentUserId " +
           "LEFT JOIN Block b ON b.blocked.id = p.author.id AND b.blocker.id = :currentUserId " +
           "WHERE p.channel.id = :channelId AND h.id IS NULL AND b.id IS NULL " +
           "AND p.contentType = 'feed' AND p.status = 'active' ORDER BY p.id DESC")
    Slice<Post> findByChannelIdFirstPage(@Param("channelId") Long channelId, @Param("currentUserId") Long currentUserId, Pageable pageable);

    @Query("SELECT p FROM Post p " +
           "LEFT JOIN Hidden h ON h.targetId = p.id AND h.targetType = p.contentType AND h.user.id = :currentUserId " +
           "LEFT JOIN Block b ON b.blocked.id = p.author.id AND b.blocker.id = :currentUserId " +
           "WHERE p.channel.id = :channelId AND p.id < :lastPostId AND h.id IS NULL AND b.id IS NULL " +
           "AND p.contentType = 'feed' AND p.status = 'active' ORDER BY p.id DESC")
    Slice<Post> findByChannelIdCursor(@Param("channelId") Long channelId, @Param("lastPostId") Long lastPostId, @Param("currentUserId") Long currentUserId, Pageable pageable);

    // 채널 인기순 (offset 기반) - likeCount DESC, viewCount DESC, id DESC
    @Query("SELECT p FROM Post p " +
           "LEFT JOIN Hidden h ON h.targetId = p.id AND h.targetType = p.contentType AND h.user.id = :currentUserId " +
           "LEFT JOIN Block b ON b.blocked.id = p.author.id AND b.blocker.id = :currentUserId " +
           "WHERE p.channel.id = :channelId AND h.id IS NULL AND b.id IS NULL " +
           "AND p.contentType = 'feed' AND p.status = 'active' ORDER BY p.likeCount DESC, p.viewCount DESC, p.id DESC")
    Page<Post> findByChannelIdPopular(@Param("channelId") Long channelId, @Param("currentUserId") Long currentUserId, Pageable pageable);

    // ALGORITHM 탭: 모든 게시물을 관심 태그 매칭 수 기준으로 정렬 (매칭 많은 것 먼저)
    @Query(value = "SELECT p FROM Post p " +
           "LEFT JOIN p.contentTags ct ON ct.tag.id IN :tagIds " +
           "LEFT JOIN Hidden h ON h.targetId = p.id AND h.targetType = p.contentType AND h.user.id = :currentUserId " +
           "LEFT JOIN Block b ON b.blocked.id = p.author.id AND b.blocker.id = :currentUserId " +
           "WHERE h.id IS NULL AND b.id IS NULL AND " +
           "p.contentType = 'feed' AND p.status = 'active' AND " +
           "(p.channel IS NULL OR p.channel.status = 'active') " +
           "GROUP BY p " +
           "ORDER BY COUNT(ct) DESC, p.likeCount DESC, p.createdAt DESC",
           countQuery = "SELECT COUNT(DISTINCT p) FROM Post p " +
           "LEFT JOIN Hidden h ON h.targetId = p.id AND h.targetType = p.contentType AND h.user.id = :currentUserId " +
           "LEFT JOIN Block b ON b.blocked.id = p.author.id AND b.blocker.id = :currentUserId " +
           "WHERE h.id IS NULL AND b.id IS NULL AND " +
           "p.contentType = 'feed' AND p.status = 'active' AND " +
           "(p.channel IS NULL OR p.channel.status = 'active')")
    Page<Post> findAlgorithmPosts(@Param("tagIds") List<Long> tagIds, @Param("currentUserId") Long currentUserId, Pageable pageable);

    // SUBSCRIBED 탭 첫 페이지: 구독 채널 필터 커서 기반
    @Query("SELECT p FROM Post p " +
           "LEFT JOIN Hidden h ON h.targetId = p.id AND h.targetType = p.contentType AND h.user.id = :currentUserId " +
           "LEFT JOIN Block b ON b.blocked.id = p.author.id AND b.blocker.id = :currentUserId " +
           "WHERE p.channel.id IN :channelIds AND h.id IS NULL AND b.id IS NULL AND " +
           "p.contentType = 'feed' AND p.status = 'active' AND p.channel.status = 'active' " +
           "ORDER BY p.id DESC")
    Slice<Post> findSubscribedPostsFirstPage(@Param("channelIds") List<Long> channelIds, @Param("currentUserId") Long currentUserId, Pageable pageable);

    // SUBSCRIBED 탭 이후 페이지: 커서 기반
    @Query("SELECT p FROM Post p " +
           "LEFT JOIN Hidden h ON h.targetId = p.id AND h.targetType = p.contentType AND h.user.id = :currentUserId " +
           "LEFT JOIN Block b ON b.blocked.id = p.author.id AND b.blocker.id = :currentUserId " +
           "WHERE p.channel.id IN :channelIds AND p.id < :lastPostId AND h.id IS NULL AND b.id IS NULL AND " +
           "p.contentType = 'feed' AND p.status = 'active' AND p.channel.status = 'active' " +
           "ORDER BY p.id DESC")
    Slice<Post> findSubscribedPostsCursor(@Param("channelIds") List<Long> channelIds, @Param("lastPostId") Long lastPostId, @Param("currentUserId") Long currentUserId, Pageable pageable);

    @Query("SELECT p FROM Post p " +
           "LEFT JOIN p.channel ch " +
           "JOIN Bookmark bm ON p.id = bm.targetId AND bm.targetType = p.contentType " +
           "LEFT JOIN Hidden h ON h.targetId = p.id AND h.targetType = p.contentType AND h.user.id = :currentUserId " +
           "LEFT JOIN Block b ON b.blocked.id = p.author.id AND b.blocker.id = :currentUserId " +
           "WHERE bm.user.id = :userId AND p.contentType IN :contentTypes AND p.status IN ('active', 'frozen') AND h.id IS NULL AND b.id IS NULL " +
           "AND (p.channel IS NULL OR ch.status = 'active')")
    Page<Post> findBookmarkedPostsByUser(@Param("userId") Long userId, @Param("contentTypes") List<String> contentTypes, @Param("currentUserId") Long currentUserId, Pageable pageable);

    // ─── 인기 탭: DB에서 점수 기반 정렬 (native SQL) ────────────────────────────

    @Query(value =
        "SELECT p.* FROM post p " +
        "LEFT JOIN hidden h ON h.target_id = p.post_id AND h.user_id = :userId " +
        "LEFT JOIN block b ON b.blocked_id = p.user_id AND b.blocker_id = :userId " +
        "WHERE h.hidden_id IS NULL AND b.block_id IS NULL " +
        "AND p.content_type = 'feed' AND p.status = 'active' " +
        "AND (p.channel_id IS NULL OR EXISTS " +
        "  (SELECT 1 FROM channel c WHERE c.channel_id = p.channel_id AND c.status = 'active')) " +
        "ORDER BY (p.like_count * 2.0 + p.comment_count * 3.0 + p.bookmark_count * 4.0 " +
        "          + p.view_count * 0.1 " +
        "          - TIMESTAMPDIFF(HOUR, p.created_at, NOW()) * 0.5) DESC",
        countQuery =
        "SELECT COUNT(*) FROM post p " +
        "LEFT JOIN hidden h ON h.target_id = p.post_id AND h.user_id = :userId " +
        "LEFT JOIN block b ON b.blocked_id = p.user_id AND b.blocker_id = :userId " +
        "WHERE h.hidden_id IS NULL AND b.block_id IS NULL " +
        "AND p.content_type = 'feed' AND p.status = 'active'",
        nativeQuery = true)
    Page<Post> findPopularPosts(@Param("userId") Long userId, Pageable pageable);

    // userId 없을 때(비로그인)용
    @Query(value =
        "SELECT p.* FROM post p " +
        "WHERE p.content_type = 'feed' AND p.status = 'active' " +
        "AND (p.channel_id IS NULL OR EXISTS " +
        "  (SELECT 1 FROM channel c WHERE c.channel_id = p.channel_id AND c.status = 'active')) " +
        "ORDER BY (p.like_count * 2.0 + p.comment_count * 3.0 + p.bookmark_count * 4.0 " +
        "          + p.view_count * 0.1 " +
        "          - TIMESTAMPDIFF(HOUR, p.created_at, NOW()) * 0.5) DESC",
        countQuery = "SELECT COUNT(*) FROM post p WHERE p.content_type = 'feed' AND p.status = 'active'",
        nativeQuery = true)
    Page<Post> findPopularPostsAnonymous(Pageable pageable);

    // ─── 알고리즘/구독 탭: 후보군 조회 (in-memory 정렬용) ────────────────────────

    @Query("SELECT p FROM Post p " +
           "LEFT JOIN Hidden h ON h.targetId = p.id AND h.user.id = :userId " +
           "LEFT JOIN Block b ON b.blocked.id = p.author.id AND b.blocker.id = :userId " +
           "WHERE h.id IS NULL AND b.id IS NULL " +
           "AND p.contentType = 'feed' AND p.status = 'active' " +
           "AND p.createdAt >= :since " +
           "AND (p.channel IS NULL OR p.channel.status = 'active') " +
           "ORDER BY p.createdAt DESC")
    List<Post> findCandidatesForAlgorithm(@Param("userId") Long userId,
                                          @Param("since") LocalDateTime since,
                                          Pageable pageable);

    @Query("SELECT p FROM Post p " +
           "LEFT JOIN Hidden h ON h.targetId = p.id AND h.user.id = :userId " +
           "LEFT JOIN Block b ON b.blocked.id = p.author.id AND b.blocker.id = :userId " +
           "WHERE h.id IS NULL AND b.id IS NULL " +
           "AND p.contentType = 'feed' AND p.status = 'active' " +
           "AND p.channel.id IN :channelIds " +
           "AND p.createdAt >= :since " +
           "AND p.channel.status = 'active' " +
           "ORDER BY p.createdAt DESC")
    List<Post> findCandidatesForSubscribed(@Param("userId") Long userId,
                                           @Param("channelIds") List<Long> channelIds,
                                           @Param("since") LocalDateTime since,
                                           Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Post p SET p.bookmarkCount = p.bookmarkCount + :delta WHERE p.id = :postId")
    void updateBookmarkCount(@Param("postId") Long postId, @Param("delta") int delta);
}
