package com.example.demo.domain.user.repository;

import com.example.demo.domain.user.entity.Interest;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.content.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterestRepository extends JpaRepository<Interest, Long> {
    Optional<Interest> findByUserAndTag(User user, Tag tag);
    List<Interest> findByUser_Id(Long userId);
    List<Interest> findByUserIdAndIsProfileTagTrue(Long userId);
    void deleteByUserIdAndIsProfileTagTrue(Long userId);
}