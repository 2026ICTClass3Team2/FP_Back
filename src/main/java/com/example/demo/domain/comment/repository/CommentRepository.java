package com.example.demo.domain.comment.repository;

import com.example.demo.domain.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 부모 댓글 조회 (최신순)
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.author WHERE c.post.id = :postId AND c.parent IS NULL ORDER BY c.createdAt DESC")
    List<Comment> findRootCommentsByPostId(@Param("postId") Long postId);

    // 대댓글 조회 (등록순)
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.author WHERE c.parent.id IN :parentIds ORDER BY c.createdAt ASC")
    List<Comment> findRepliesByParentIds(@Param("parentIds") List<Long> parentIds);

    // 차단 유저의 루트 댓글 수 집계 (postId별)
    @Query("SELECT c.post.id, COUNT(c) FROM Comment c WHERE c.post.id IN :postIds AND c.author.id IN :blockedUserIds AND c.parent IS NULL AND c.status = 'active' GROUP BY c.post.id")
    List<Object[]> countBlockedRootCommentsByPostIds(@Param("postIds") List<Long> postIds, @Param("blockedUserIds") List<Long> blockedUserIds);
}
