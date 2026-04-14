package com.example.demo.domain.content.feed.repository;

import com.example.demo.domain.content.entity.Post;
import com.example.demo.domain.content.enums.PostStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 무한 스크롤용 - Cursor 기반 페이징 (No-Offset)
    @Query("SELECT p FROM Post p WHERE p.id < :lastPostId AND p.status = com.example.demo.domain.content.enums.PostStatus.active ORDER BY p.id DESC")
    Slice<Post> findPostsByCursor(@Param("lastPostId") Long lastPostId, Pageable pageable);

    // 무한 스크롤용 - 첫 페이지 조회 (lastPostId가 없을 때)
    @Query("SELECT p FROM Post p WHERE p.status = com.example.demo.domain.content.enums.PostStatus.active ORDER BY p.id DESC")
    Slice<Post> findPostsFirstPage(Pageable pageable);

    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
    void increaseViewCount(@Param("postId") Long postId);

    Optional<Post> findByIdAndStatus(Long id, PostStatus status);
}
