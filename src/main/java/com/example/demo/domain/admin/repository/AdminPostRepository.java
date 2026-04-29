package com.example.demo.domain.admin.repository;

import com.example.demo.domain.content.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminPostRepository extends JpaRepository<Post, Long> {
    // 기존: 모든 공지 다 가져오기
    List<Post> findAllByContentTypeOrderByCreatedAtDesc(String contentType);

    // ✅ 추가: 특정 상태(active)인 공지만 생성일 역순으로 가져오기
    List<Post> findAllByContentTypeAndStatusOrderByCreatedAtDesc(String contentType, String status);
}