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
    @Query("SELECT p FROM Post p WHERE p.id < :lastPostId ORDER BY p.id DESC")
    Slice<Post> findPostsByCursor(@Param("lastPostId") Long lastPostId, Pageable pageable);

    // 무한 스크롤용 - 첫 페이지 조회 (lastPostId가 없을 때)
    @Query("SELECT p FROM Post p ORDER BY p.id DESC")
    Slice<Post> findPostsFirstPage(Pageable pageable);

    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
    void increaseViewCount(@Param("postId") Long postId);

    Page<Post> findByAuthorIdAndContentTypeIn(Long authorId, List<String> contentTypes, Pageable pageable);

    @Query("SELECT p FROM Post p JOIN Bookmark b ON p.id = b.targetId WHERE b.user.id = :userId AND p.contentType IN :contentTypes")
    Page<Post> findBookmarkedPostsByUser(@Param("userId") Long userId, @Param("contentTypes") List<String> contentTypes, Pageable pageable);
}
