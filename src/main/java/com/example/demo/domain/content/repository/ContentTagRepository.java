package com.example.demo.domain.content.repository;

import com.example.demo.domain.content.entity.ContentTag;
import com.example.demo.domain.content.entity.Post;
import com.example.demo.domain.draft.entity.Draft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentTagRepository extends JpaRepository<ContentTag, Long> {
    void deleteAllByPost(Post post);
    List<ContentTag> findByPost_Id(Long postId);
    List<ContentTag> findByPost_IdIn(List<Long> postIds);
    void deleteAllByDraft(Draft draft);
    List<ContentTag> findByDraft_Id(Long draftId);
}
