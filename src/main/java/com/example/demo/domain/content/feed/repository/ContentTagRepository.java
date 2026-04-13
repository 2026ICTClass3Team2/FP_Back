package com.example.demo.domain.content.feed.repository;

import com.example.demo.domain.content.feed.entity.ContentTag;
import com.example.demo.domain.content.feed.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentTagRepository extends JpaRepository<ContentTag, Long> {
    void deleteAllByPost(Post post);
}
