package com.example.demo.domain.interaction.repository;

import com.example.demo.domain.interaction.entity.ViewHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ViewHistoryRepository extends JpaRepository<ViewHistory, Long> {

    // 1시간 이내 같은 사용자+게시글 조회 이력 존재 여부 (중복 방지)
    boolean existsByUser_IdAndPostIdAndViewedAtAfter(Long userId, Long postId, LocalDateTime threshold);
}
