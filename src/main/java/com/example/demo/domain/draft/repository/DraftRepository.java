package com.example.demo.domain.draft.repository;

import com.example.demo.domain.draft.entity.Draft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DraftRepository extends JpaRepository<Draft, Long> {
    List<Draft> findByUserEmailAndTargetTypeOrderByCreatedAtDesc(String email, String targetType);
    Optional<Draft> findByIdAndUserEmail(Long id, String email);
    void deleteByIdAndUserEmail(Long id, String email);
}
