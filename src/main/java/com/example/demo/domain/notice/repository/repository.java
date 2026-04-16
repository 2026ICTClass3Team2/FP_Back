package com.example.demo.domain.notice.repository;

import com.example.demo.domain.notice.entity.postEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface repository extends JpaRepository<postEntity, Long> {
}