package com.example.demo.domain.notice.repository;

import com.example.demo.domain.content.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface NoticeRepository extends JpaRepository<Post, Long> {

    boolean existsByTitle(String title);

    // 🔴 조회수 1 증가 쿼리 추가
    @Modifying
    @Transactional // 트랜잭션이 필수
    @Query("UPDATE Post p SET p.viewCount = COALESCE(p.viewCount, 0) + 1 WHERE p.id = :id")
    int incrementViewCount(@Param("id") Long id);
}