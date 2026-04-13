package com.example.demo.domain.content.feed.service;

import com.example.demo.domain.content.feed.dto.PostCreateRequestDto;
import com.example.demo.domain.content.feed.dto.PostDetailResponseDto;
import com.example.demo.domain.content.feed.dto.PostFeedResponseDto;
import com.example.demo.domain.content.feed.dto.PostUpdateRequestDto;
import org.springframework.data.domain.Slice;

public interface PostService {
    
    // 무한 스크롤 커서 페이징 방식의 게시물 조회
    Slice<PostFeedResponseDto> getPostsFeed(Long lastPostId, int size, String currentUsername);

    // 단건 조회
    PostDetailResponseDto getPostDetail(Long postId, String currentUsername);

    // 게시물 삭제
    void deletePost(Long postId, String currentUsername);

    // 좋아요
    void likePost(Long postId);

    // 비추천
    void dislikePost(Long postId);

    // 북마크 토글 기능
    boolean toggleBookmark(Long postId, String currentUsername);
    
    // 게시물 생성
    Long createPost(PostCreateRequestDto requestDto, String currentUsername);

    // 게시물 수정
    void updatePost(Long postId, PostUpdateRequestDto requestDto, String currentUsername);
}
