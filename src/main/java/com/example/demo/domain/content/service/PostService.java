package com.example.demo.domain.content.service;

import com.example.demo.domain.content.dto.PostCreateRequestDto;
import com.example.demo.domain.content.dto.PostDetailResponseDto;
import com.example.demo.domain.content.dto.PostFeedResponseDto;
import com.example.demo.domain.content.dto.PostUpdateRequestDto;
import org.springframework.data.domain.Slice;

public interface PostService {
    
    Slice<PostFeedResponseDto> getPostsFeed(Long lastPostId, int size, String currentUsername);

    PostDetailResponseDto getPostDetail(Long postId, String currentUsername);

    void deletePost(Long postId, String currentUsername);

    void toggleInteraction(Long postId, String actionType, String currentUsername);

    boolean toggleBookmark(Long postId, String currentUsername);
    
    Long createPost(PostCreateRequestDto requestDto, String currentUsername);

    void updatePost(Long postId, PostUpdateRequestDto requestDto, String currentUsername);

    String getContentType(Long postId);

    void increaseViewCount(Long postId, Long userId);
}
