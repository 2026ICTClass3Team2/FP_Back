package com.example.demo.domain.notice.repository;

import com.example.demo.domain.content.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminNoticeRepository extends JpaRepository<Post, Long> {
}
