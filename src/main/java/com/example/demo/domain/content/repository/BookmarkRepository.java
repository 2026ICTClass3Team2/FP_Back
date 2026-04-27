package com.example.demo.domain.content.repository;

import com.example.demo.domain.content.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    @Query("SELECT b FROM Bookmark b WHERE b.user.id = :userId AND b.targetId = :targetId AND b.targetType = :targetType")
    Optional<Bookmark> findByUserIdAndTargetIdAndTargetType(@Param("userId") Long userId, @Param("targetId") Long targetId, @Param("targetType") String targetType);

    @Query("SELECT COUNT(b) > 0 FROM Bookmark b WHERE b.user.id = :userId AND b.targetId = :targetId AND b.targetType = :targetType")
    boolean existsByUserIdAndTargetIdAndTargetType(@Param("userId") Long userId, @Param("targetId") Long targetId, @Param("targetType") String targetType);

    long countByTargetIdAndTargetType(Long targetId, String targetType);
}
