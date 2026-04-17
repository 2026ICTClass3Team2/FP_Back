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

    // 무한 스크롤용 - Cursor 기반 페이징 (No-Offset)
    @Query("SELECT p FROM Post p " +
           "LEFT JOIN Hidden h ON h.targetId = p.id AND h.user.id = :currentUserId " +
           "LEFT JOIN Block b ON b.blocked.id = p.author.id AND b.blocker.id = :currentUserId " +
           "WHERE p.id < :lastPostId AND h.id IS NULL AND b.id IS NULL AND " +
           "p.contentType = 'feed' AND p.status = 'active'" +
           "ORDER BY p.id DESC")
    Slice<Post> findPostsByCursor(@Param("lastPostId") Long lastPostId, @Param("currentUserId") Long currentUserId, Pageable pageable);

    // 무한 스크롤용 - 첫 페이지 조회 (lastPostId가 없을 때)
    @Query("SELECT p FROM Post p " +
           "LEFT JOIN Hidden h ON h.targetId = p.id AND h.user.id = :currentUserId " +
           "LEFT JOIN Block b ON b.blocked.id = p.author.id AND b.blocker.id = :currentUserId " +
           "WHERE h.id IS NULL AND b.id IS NULL AND " +
            "p.contentType = 'feed' AND p.status = 'active'" +
           "ORDER BY p.id DESC")
    Slice<Post> findPostsFirstPage(@Param("currentUserId") Long currentUserId, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
    void increaseViewCount(@Param("postId") Long postId);

    @Query("SELECT p FROM Post p " +
           "LEFT JOIN Hidden h ON h.targetId = p.id AND h.user.id = :currentUserId " +
           "LEFT JOIN Block b ON b.blocked.id = p.author.id AND b.blocker.id = :currentUserId " +
           "WHERE p.author.id = :authorId AND p.contentType IN :contentTypes AND h.id IS NULL AND b.id IS NULL")
    Page<Post> findByAuthorIdAndContentTypeIn(@Param("authorId") Long authorId, @Param("contentTypes") List<String> contentTypes, @Param("currentUserId") Long currentUserId, Pageable pageable);

    @Query("SELECT p FROM Post p " +
           "JOIN Bookmark bm ON p.id = bm.targetId " +
           "LEFT JOIN Hidden h ON h.targetId = p.id AND h.user.id = :currentUserId " +
           "LEFT JOIN Block b ON b.blocked.id = p.author.id AND b.blocker.id = :currentUserId " +
           "WHERE bm.user.id = :userId AND p.contentType IN :contentTypes AND h.id IS NULL AND b.id IS NULL")
    Page<Post> findBookmarkedPostsByUser(@Param("userId") Long userId, @Param("contentTypes") List<String> contentTypes, @Param("currentUserId") Long currentUserId, Pageable pageable);
}
