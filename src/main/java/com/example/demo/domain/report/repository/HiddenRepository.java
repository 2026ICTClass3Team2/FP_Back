package com.example.demo.domain.report.repository;

import com.example.demo.domain.report.entity.Hidden;
import com.example.demo.domain.report.enums.HiddenTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HiddenRepository extends JpaRepository<Hidden, Long> {

    boolean existsByUserIdAndTargetId(Long userId, Long targetId);

    boolean existsByUserIdAndTargetIdAndTargetType(Long userId, Long targetId, HiddenTargetType targetType);

    void deleteByUserIdAndTargetIdAndTargetType(Long userId, Long targetId, HiddenTargetType targetType);

    @Query("SELECT h.targetId FROM Hidden h WHERE h.user.id = :userId AND h.targetType = :targetType")
    List<Long> findTargetIdsByUserIdAndTargetType(@Param("userId") Long userId, @Param("targetType") HiddenTargetType targetType);
}
