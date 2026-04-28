package com.example.demo.domain.user.repository;

import com.example.demo.domain.user.entity.Interest;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.content.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InterestRepository extends JpaRepository<Interest, Long> {
    Optional<Interest> findByUserAndTag(User user, Tag tag);
    List<Interest> findByUser_Id(Long userId);
    List<Interest> findByUserId(Long userId);
    List<Interest> findByUserIdAndIsProfileTagTrue(Long userId);
    void deleteByUserIdAndIsProfileTagTrue(Long userId);

    @Modifying
    @Query("UPDATE Interest i SET i.weightScore = i.weightScore * :factor, i.lastInteractionAt = :now WHERE i.isProfileTag = false")
    int decayAllNonProfileInterests(double factor, LocalDateTime now);

    @Modifying
    @Query("DELETE FROM Interest i WHERE i.isProfileTag = false AND i.weightScore < :threshold")
    int deleteByWeightScoreLessThanAndIsProfileTagFalse(double threshold);
}