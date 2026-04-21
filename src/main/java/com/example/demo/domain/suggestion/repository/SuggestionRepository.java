package com.example.demo.domain.suggestion.repository;

import com.example.demo.domain.suggestion.entity.Suggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SuggestionRepository extends JpaRepository<Suggestion, Long> {
    long countByIsSeenFalse();
    List<Suggestion> findAllByOrderByIsSeenAscIdDesc();
    List<Suggestion> findByIsSeenOrderByIdDesc(Boolean isSeen);
}
