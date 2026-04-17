package com.example.demo.domain.report.repository;

import com.example.demo.domain.report.entity.Block;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockRepository extends JpaRepository<Block, Long> {
    Page<Block> findByBlockerId(Long blockerId, Pageable pageable);
}
