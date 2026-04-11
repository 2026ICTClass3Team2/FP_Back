package com.example.demo.domain.content.repository;

import com.example.demo.domain.content.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    Optional<Bookmark> findByUserIdAndTargetIdAndTargetType(Long userId, Long targetId, String targetType);
    boolean existsByUserIdAndTargetIdAndTargetType(Long userId, Long targetId, String targetType);
}
