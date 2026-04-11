package com.example.demo.domain.content.service;

import com.example.demo.domain.content.dto.PostCreateRequestDto;
import com.example.demo.domain.content.dto.PostFeedResponseDto;
import com.example.demo.domain.content.dto.PostUpdateRequestDto;
import org.springframework.data.domain.Slice;

public interface PostService {
    
    // 무한 스크롤 커서 페이징 방식의 게시물 조회
    Slice<PostFeedResponseDto> getPostsFeed(Long lastPostId, int size, String currentUsername);

    // 게시물 삭제
    void deletePost(Long postId, String currentUsername);

    // 좋아요 토글 기능
    boolean toggleLike(Long postId, String currentUsername);

    // 북마크 토글 기능
    boolean toggleBookmark(Long postId, String currentUsername);
    
    // 게시물 생성
    Long createPost(PostCreateRequestDto requestDto, String currentUsername);

    // 게시물 수정
    void updatePost(Long postId, PostUpdateRequestDto requestDto, String currentUsername);
}
