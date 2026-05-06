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

    // 차단 유저의 루트 댓글 수 집계 (postId별) — 기존 호환성 유지
    @Query("SELECT c.post.id, COUNT(c) FROM Comment c WHERE c.post.id IN :postIds AND c.author.id IN :blockedUserIds AND c.parent IS NULL AND c.status = 'active' GROUP BY c.post.id")
    List<Object[]> countBlockedRootCommentsByPostIds(@Param("postIds") List<Long> postIds, @Param("blockedUserIds") List<Long> blockedUserIds);

    // 차단 유저의 전체 댓글 수 집계 (루트 + 대댓글, postId별)
    @Query("SELECT c.post.id, COUNT(c) FROM Comment c WHERE c.post.id IN :postIds AND c.author.id IN :blockedUserIds AND c.status = 'active' GROUP BY c.post.id")
    List<Object[]> countAllBlockedCommentsByPostIds(@Param("postIds") List<Long> postIds, @Param("blockedUserIds") List<Long> blockedUserIds);

    // 실제로 숨겨지는 댓글 전체 집계 (차단유저 작성 + 차단유저 루트 하위 비차단 대댓글)
    // 단일 네이티브 쿼리로 OR 조건을 합산 — 이중 집계 없음
    @Query(nativeQuery = true, value =
        "SELECT c.post_id, COUNT(c.comment_id) " +
        "FROM comment c " +
        "WHERE c.post_id IN :postIds " +
        "AND c.status = 'active' " +
        "AND (" +
        "  c.user_id IN :blockedUserIds " +
        "  OR c.parent_id IN (" +
        "    SELECT p.comment_id FROM comment p " +
        "    WHERE p.post_id IN :postIds " +
        "    AND p.user_id IN :blockedUserIds " +
        "    AND p.parent_id IS NULL " +
        "    AND p.status = 'active'" +
        "  )" +
        ") " +
        "GROUP BY c.post_id")
    List<Object[]> countAllHiddenCommentsByPostIds(@Param("postIds") List<Long> postIds, @Param("blockedUserIds") List<Long> blockedUserIds);

    // 차단 유저의 루트 댓글 하위에 달린 비차단 유저의 대댓글 수 집계 (네이티브 SQL — 컬럼명 직접 참조)
    @Query(nativeQuery = true, value =
        "SELECT r.post_id, COUNT(r.comment_id) " +
        "FROM comment r " +
        "INNER JOIN comment p ON r.parent_id = p.comment_id " +
        "WHERE r.post_id IN :postIds " +
        "AND r.user_id NOT IN :blockedUserIds " +
        "AND r.status = 'active' " +
        "AND p.parent_id IS NULL " +
        "AND p.user_id IN :blockedUserIds " +
        "AND p.status = 'active' " +
        "GROUP BY r.post_id")
    List<Object[]> countNonBlockedRepliesUnderBlockedRootsByPostIds(@Param("postIds") List<Long> postIds, @Param("blockedUserIds") List<Long> blockedUserIds);

    // 특정 게시글의 활성 루트 댓글 수 조회 (수정/삭제 가능 여부 판단용)
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.id = :postId AND c.parent IS NULL AND c.status = 'active'")
    long countActiveRootCommentsByPostId(@Param("postId") Long postId);

    // Bug 4: 작성자 본인 댓글을 제외한 활성 루트 댓글 수 (본인 댓글만 있으면 0)
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.id = :postId AND c.parent IS NULL AND c.status = 'active' AND c.author.id != :authorId")
    long countActiveRootCommentsByPostIdExcludingAuthor(@Param("postId") Long postId, @Param("authorId") Long authorId);
}
