package com.example.demo.domain.admin.repository;

import com.example.demo.domain.content.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminPostRepository extends JpaRepository<Post, Long> {
    List<Post> findAllByContentTypeOrderByCreatedAtDesc(String contentType);
}