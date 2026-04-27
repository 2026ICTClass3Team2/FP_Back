package com.example.demo.domain.content.controller;

import com.example.demo.domain.content.dto.PostCreateRequestDto;
import com.example.demo.domain.content.dto.PostDetailResponseDto;
import com.example.demo.domain.content.dto.PostFeedResponseDto;
import com.example.demo.domain.content.dto.PostUpdateRequestDto;
import com.example.demo.domain.content.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;

    @GetMapping
    public ResponseEntity<Slice<PostFeedResponseDto>> getPostsFeed(
            @RequestParam(defaultValue = "LATEST") String tab,
    public ResponseEntity<?> getPostsFeed(
            @RequestParam(defaultValue = "LATEST") FeedTab tab,
            @RequestParam(required = false) Long lastPostId,
            @RequestParam(required = false) Integer page,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        String currentUsername = (userDetails != null) ? userDetails.getUsername() : null;
        Object result = postService.getFeedByTab(tab, lastPostId, page, size, currentUsername);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{postId}/not-interested")
    public ResponseEntity<?> notInterested(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Slice<PostFeedResponseDto> posts = postService.getPostsFeed(tab, lastPostId, page, size, currentUsername);
        return ResponseEntity.ok(posts);
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        postService.notInterested(postId, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "해당 게시글을 더 이상 추천하지 않습니다."));
    }

    @PostMapping("/{postId}/share")
    public ResponseEntity<?> trackShare(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String currentUsername = (userDetails != null) ? userDetails.getUsername() : null;
        postService.trackShare(postId, currentUsername);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{postId}")
    public ResponseEntity<?> getPostDetail(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String currentUsername = (userDetails != null) ? userDetails.getUsername() : null;
        try {
            PostDetailResponseDto detailDto = postService.getPostDetail(postId, currentUsername);
            return ResponseEntity.ok(detailDto);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(410).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createPost(
            @Valid @RequestBody PostCreateRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }

        try {
            Long postId = postService.createPost(requestDto, userDetails.getUsername());
            return ResponseEntity.ok(Map.of("postId", postId, "message", "게시글이 성공적으로 작성되었습니다."));
        } catch (Exception e) {
            log.error("Failed to create post", e);
            return ResponseEntity.badRequest().body(Map.of("message", "게시글 작성에 실패했습니다."));
        }
    }

    @PutMapping("/{postId}")
    public ResponseEntity<?> updatePost(
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }

        try {
            postService.updatePost(postId, requestDto, userDetails.getUsername());
            return ResponseEntity.ok(Map.of("postId", postId, "message", "게시글이 성공적으로 수정되었습니다."));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("message", "수정 권한이 없습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update post", e);
            return ResponseEntity.badRequest().body(Map.of("message", "게시글 수정에 실패했습니다."));
        }
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<?> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }

        try {
            postService.deletePost(postId, userDetails.getUsername());
            return ResponseEntity.ok(Map.of("message", "게시글이 삭제되었습니다."));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("message", "삭제 권한이 없습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<?> toggleLike(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }

        postService.toggleInteraction(postId, "like", userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{postId}/dislike")
    public ResponseEntity<?> toggleDislike(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }

        postService.toggleInteraction(postId, "dislike", userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{postId}/bookmark")
    public ResponseEntity<?> toggleBookmark(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }

        boolean isBookmarked = postService.toggleBookmark(postId, userDetails.getUsername());
        return ResponseEntity.ok(Map.of(
                "isBookmarked", isBookmarked,
                "message", isBookmarked ? "북마크에 추가되었습니다." : "북마크가 해제되었습니다."
        ));
    }
}
