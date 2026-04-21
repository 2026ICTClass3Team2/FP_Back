package com.example.demo.domain.follow.repository;

import com.example.demo.domain.follow.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    Optional<Follow> findByUser_IdAndTargetIdAndTargetType(Long userId, Long targetId, String targetType);
    boolean existsByUser_IdAndTargetIdAndTargetType(Long userId, Long targetId, String targetType);
    List<Follow> findByUser_IdAndTargetType(Long userId, String targetType);
    List<Follow> findByTargetIdAndTargetType(Long targetId, String targetType);
}
