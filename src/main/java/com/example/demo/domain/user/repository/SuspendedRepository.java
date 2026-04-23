package com.example.demo.domain.user.repository;

import com.example.demo.domain.user.entity.Suspended;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SuspendedRepository extends JpaRepository<Suspended, Long> {
    Optional<Suspended> findByUserIdAndReleasedAtIsNull(Long userId);
    Optional<Suspended> findTopByUserIdOrderBySuspendedAtDesc(Long userId);
}
