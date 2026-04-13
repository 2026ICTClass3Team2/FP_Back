package com.example.demo.domain.comment.repository;

import com.example.demo.domain.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.author WHERE c.post.id = :postId ORDER BY c.parent.id ASC NULLS FIRST, c.createdAt ASC")
    List<Comment> findByPostIdWithAuthor(@Param("postId") Long postId);
}
