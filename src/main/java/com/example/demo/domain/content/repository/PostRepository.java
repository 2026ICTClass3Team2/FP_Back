package com.example.demo.domain.content.repository;

import com.example.demo.domain.content.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
           "LEFT JOIN Hidden h ON h.targetId = p.id AND h.targetType = p.contentType AND h.user.id = :currentUserId " +
           "LEFT JOIN Block b ON b.blocked.id = p.author.id AND b.blocker.id = :currentUserId " +
           "WHERE p.author.id = :authorId AND p.contentType IN :contentTypes AND p.status IN ('active', 'frozen') AND h.id IS NULL AND b.id IS NULL " +
           "AND (p.channel IS NULL OR p.channel.status = 'active')")
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

    // POPULAR 탭: likeCount DESC 오프셋 페이지네이션
    @Query("SELECT p FROM Post p " +
           "LEFT JOIN Hidden h ON h.targetId = p.id AND h.targetType = p.contentType AND h.user.id = :currentUserId " +
           "LEFT JOIN Block b ON b.blocked.id = p.author.id AND b.blocker.id = :currentUserId " +
           "WHERE h.id IS NULL AND b.id IS NULL AND " +
           "p.contentType = 'feed' AND p.status = 'active' AND " +
           "(p.channel IS NULL OR p.channel.status = 'active') " +
           "ORDER BY p.likeCount DESC, p.viewCount DESC, p.id DESC")
    Page<Post> findPopularPosts(@Param("currentUserId") Long currentUserId, Pageable pageable);

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
           "JOIN Bookmark bm ON p.id = bm.targetId " +
           "LEFT JOIN Hidden h ON h.targetId = p.id AND h.targetType = p.contentType AND h.user.id = :currentUserId " +
           "LEFT JOIN Block b ON b.blocked.id = p.author.id AND b.blocker.id = :currentUserId " +
           "WHERE bm.user.id = :userId AND p.contentType IN :contentTypes AND p.status IN ('active', 'frozen') AND h.id IS NULL AND b.id IS NULL " +
           "AND (p.channel IS NULL OR p.channel.status = 'active')")
    Page<Post> findBookmarkedPostsByUser(@Param("userId") Long userId, @Param("contentTypes") List<String> contentTypes, @Param("currentUserId") Long currentUserId, Pageable pageable);
}
