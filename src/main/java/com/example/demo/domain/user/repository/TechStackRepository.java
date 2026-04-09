package com.example.demo.domain.user.repository;

import com.example.demo.domain.user.entity.TechStack;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TechStackRepository extends JpaRepository<TechStack, Long> {
}
