// package com.example.demo.domain.interaction.repository;

// import com.example.demo.domain.interaction.entity.ViewHistory;
// import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.stereotype.Repository;

// import java.time.LocalDateTime;

// @Repository
// public interface ViewHistoryRepository extends JpaRepository<ViewHistory, Long> {
//     boolean existsByUserIdAndTargetTypeAndTargetIdAndCreatedAtAfter(
//             Long userId, String targetType, Long targetId, LocalDateTime thresholdTime);
// }
