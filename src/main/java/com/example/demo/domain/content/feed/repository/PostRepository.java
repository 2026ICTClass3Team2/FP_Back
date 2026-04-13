package com.example.demo.domain.content.feed.repository;

import com.example.demo.domain.content.feed.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 무한 스크롤용 - Cursor 기반 페이징 (No-Offset)
    @Query("SELECT p FROM Post p WHERE p.id < :lastPostId ORDER BY p.id DESC")
    Slice<Post> findPostsByCursor(@Param("lastPostId") Long lastPostId, Pageable pageable);

    // 무한 스크롤용 - 첫 페이지 조회 (lastPostId가 없을 때)
    @Query("SELECT p FROM Post p ORDER BY p.id DESC")
    Slice<Post> findPostsFirstPage(Pageable pageable);
}
