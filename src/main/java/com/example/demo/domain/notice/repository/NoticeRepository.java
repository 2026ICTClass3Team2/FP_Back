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

    // 조회수 1 증가 쿼리
    @Modifying
    @Transactional // 필수적으로 사용
    // JPQL 사용
    @Query("UPDATE Post p SET p.viewCount = COALESCE(p.viewCount, 0) + 1 WHERE p.id = :id")
    int incrementViewCount(@Param("id") Long id);
}