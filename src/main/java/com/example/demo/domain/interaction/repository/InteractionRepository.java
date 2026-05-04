package com.example.demo.domain.interaction.repository;

import com.example.demo.domain.interaction.entity.Interaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface InteractionRepository extends JpaRepository<Interaction, Long> {
    Optional<Interaction> findByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId);
    List<Interaction> findByUserIdAndTargetTypeAndTargetIdIn(Long userId, String targetType, Collection<Long> targetIds);
}
