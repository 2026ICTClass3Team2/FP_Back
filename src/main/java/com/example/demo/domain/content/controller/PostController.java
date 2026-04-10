package com.example.demo.domain.content.controller;

import com.example.demo.domain.content.dto.PostCreateRequestDto;
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
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    // 1. 게시글 무한 스크롤 조회 (커서 페이징)
    @GetMapping
    public ResponseEntity<Slice<PostFeedResponseDto>> getPostsFeed(
            @RequestParam(required = false) Long lastPostId,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String currentUsername = (userDetails != null) ? userDetails.getUsername() : null;
        
        Slice<PostFeedResponseDto> posts = postService.getPostsFeed(lastPostId, size, currentUsername);
        return ResponseEntity.ok(posts);
    }

    // 2. 게시글 작성
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

    // 3. 게시글 수정 (isAuthor 확인 필요)
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

    // 4. 게시글 삭제 (isAuthor 확인 필요)
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

    // 5. 좋아요 토글
    @PostMapping("/{postId}/like")
    public ResponseEntity<?> toggleLike(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        
        boolean isLiked = postService.toggleLike(postId, userDetails.getUsername());
        return ResponseEntity.ok(Map.of(
                "isLiked", isLiked,
                "message", isLiked ? "좋아요가 추가되었습니다." : "좋아요가 취소되었습니다."
        ));
    }

    // 6. 북마크 토글
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
