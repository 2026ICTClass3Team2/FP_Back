package com.example.demo.domain.content.repository;

import com.example.demo.domain.content.entity.ContentTag;
import com.example.demo.domain.content.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentTagRepository extends JpaRepository<ContentTag, Long> {
    void deleteAllByPost(Post post);
}
