package com.example.demo.domain.interaction.repository;

import com.example.demo.domain.interaction.entity.Interaction;
import com.example.demo.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InteractionRepository extends JpaRepository<Interaction, Long> {
    Optional<Interaction> findByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId);
}
