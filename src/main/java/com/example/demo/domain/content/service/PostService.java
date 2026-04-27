package com.example.demo.domain.content.service;

import com.example.demo.domain.algorithm.enums.FeedTab;
import com.example.demo.domain.content.dto.PostCreateRequestDto;
import com.example.demo.domain.content.dto.PostDetailResponseDto;
import com.example.demo.domain.content.dto.PostFeedResponseDto;
import com.example.demo.domain.content.dto.PostUpdateRequestDto;
import org.springframework.data.domain.Slice;

public interface PostService {

    // 탭별 피드 (알고리즘/인기/구독 → offset, 최신 → cursor)
    Object getFeedByTab(FeedTab tab, Long lastPostId, int page, int size, String currentUsername);

    Slice<PostFeedResponseDto> getPostsFeed(Long lastPostId, int size, String currentUsername);

    void notInterested(Long postId, String currentUsername);

    void trackShare(Long postId, String currentUsername);

    PostDetailResponseDto getPostDetail(Long postId, String currentUsername);

    void deletePost(Long postId, String currentUsername);

    void toggleInteraction(Long postId, String actionType, String currentUsername);

    boolean toggleBookmark(Long postId, String currentUsername);
    
    Long createPost(PostCreateRequestDto requestDto, String currentUsername);

    void updatePost(Long postId, PostUpdateRequestDto requestDto, String currentUsername);

    String getContentType(Long postId);

    void increaseViewCount(Long postId, Long userId);

    Slice<PostFeedResponseDto> getChannelPosts(Long channelId, Long lastPostId, int size, String currentUsername);
}
