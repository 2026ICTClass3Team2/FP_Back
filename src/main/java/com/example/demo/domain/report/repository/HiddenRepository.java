package com.example.demo.domain.report.repository;

import com.example.demo.domain.report.entity.Hidden;
import com.example.demo.domain.report.enums.HiddenTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HiddenRepository extends JpaRepository<Hidden, Long> {

    boolean existsByUserIdAndTargetId(Long userId, Long targetId);

    @Query("SELECT h.targetId FROM Hidden h WHERE h.user.id = :userId AND h.targetType = :targetType")
    List<Long> findTargetIdsByUserIdAndTargetType(@Param("userId") Long userId, @Param("targetType") HiddenTargetType targetType);

    // 언어 숨김 전용 — 어떤 관리자가 숨겼든 전체 목록 반환
    @Query("SELECT DISTINCT h.targetId FROM Hidden h WHERE h.targetType = :targetType")
    List<Long> findTargetIdsByTargetType(@Param("targetType") HiddenTargetType targetType);

    boolean existsByTargetTypeAndTargetId(HiddenTargetType targetType, Long targetId);

    void deleteByTargetTypeAndTargetId(HiddenTargetType targetType, Long targetId);
}
