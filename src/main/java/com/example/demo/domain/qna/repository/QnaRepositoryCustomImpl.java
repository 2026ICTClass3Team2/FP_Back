package com.example.demo.domain.qna.repository;

import com.example.demo.domain.content.entity.QPost;
import com.example.demo.domain.qna.dto.QQnaCardResponseDto;
import com.example.demo.domain.qna.dto.QnaCardResponseDto;
import com.example.demo.domain.qna.entity.QQna;
import com.example.demo.domain.user.entity.QUser;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.util.List;

@RequiredArgsConstructor
public class QnaRepositoryCustomImpl implements QnaRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<QnaCardResponseDto> findQnaList(String query, String sort, String status, Pageable pageable) {
        QQna qna = QQna.qna;
        QPost post = QPost.post;
        QUser user = QUser.user;

        List<QnaCardResponseDto> results = queryFactory
                .select(new QQnaCardResponseDto(
                        qna.id,
                        post.title,
                        post.body,
                        user.username,
                        user.nickname,
                        user.profilePicUrl,
                        qna.isSolved,
                        qna.rewardPoints,
                        post.createdAt,
                        post.commentCount,
                        post.likeCount,
                        post.dislikeCount,
                        post.viewCount
                ))
                .from(qna)
                .join(qna.post, post)
                .join(post.author, user)
                .where(
                        searchQuery(query),
                        statusFilter(status)
                )
                .orderBy(sortOrder(sort))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(qna.count())
                .from(qna)
                .join(qna.post, post)
                .join(post.author, user)
                .where(
                        searchQuery(query),
                        statusFilter(status)
                );

        Long total = countQuery.fetchOne();

        return new PageImpl<>(results, pageable, total != null ? total : 0L);
    }

    private BooleanExpression searchQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return null;
        }
        QPost post = QPost.post;
        QUser user = QUser.user;
        return post.title.containsIgnoreCase(query)
                .or(post.body.containsIgnoreCase(query))
                .or(user.username.containsIgnoreCase(query))
                .or(user.nickname.containsIgnoreCase(query));
    }

    private BooleanExpression statusFilter(String status) {
        if (!StringUtils.hasText(status) || "all".equalsIgnoreCase(status)) {
            return null;
        }
        if ("resolved".equalsIgnoreCase(status)) {
            return QQna.qna.isSolved.isTrue();
        }
        if ("unresolved".equalsIgnoreCase(status)) {
            return QQna.qna.isSolved.isFalse();
        }
        return null;
    }

    private OrderSpecifier<?> sortOrder(String sort) {
        if (!StringUtils.hasText(sort)) {
            return QPost.post.createdAt.desc();
        }
        return switch (sort) {
            case "popular" -> QPost.post.likeCount.desc();
            case "comments" -> QPost.post.commentCount.desc();
            default -> QPost.post.createdAt.desc();
        };
    }
}
