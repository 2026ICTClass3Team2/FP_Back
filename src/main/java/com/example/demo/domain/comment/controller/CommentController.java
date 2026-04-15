package com.example.demo.domain.comment.controller;

import com.example.demo.domain.comment.dto.CommentRequestDto;
import com.example.demo.domain.comment.dto.CommentResponseDto;
import com.example.demo.domain.comment.service.CommentService;
import com.example.demo.domain.content.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final PostService postService; // Injected to verify the post's content type

    // Helper method to validate if the postId corresponds to the expected content_type
    private void validateContentType(Long postId, String expectedType) {
        // Note: Adjust 'getContentType' to match the actual method name in your PostService.
        // If your service returns the Post entity directly, you can do:
        // String actualType = postService.getPostById(postId).getContentType();
        String actualType = postService.getContentType(postId);
        
        if (!expectedType.equals(actualType)) {
            throw new IllegalArgumentException("Path mismatch: Post " + postId + " is not of type '" + expectedType + "'");
        }
    }

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentResponseDto> createPostComment(
            @PathVariable Long postId,
            @RequestBody CommentRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        validateContentType(postId, "feed");
        CommentResponseDto response = commentService.createComment(postId, requestDto, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/qna/{postId}/comments")
    public ResponseEntity<CommentResponseDto> createQnaComment(
            @PathVariable Long postId,
            @RequestBody CommentRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        validateContentType(postId, "qna");
        CommentResponseDto response = commentService.createComment(postId, requestDto, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<List<CommentResponseDto>> getPostComments(@PathVariable Long postId) {
        validateContentType(postId, "feed");
        List<CommentResponseDto> responses = commentService.getComments(postId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/qna/{postId}/comments")
    public ResponseEntity<List<CommentResponseDto>> getQnaComments(@PathVariable Long postId) {
        validateContentType(postId, "qna");
        List<CommentResponseDto> responses = commentService.getComments(postId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/posts/{postId}/comments/{commentId}")
    public ResponseEntity<CommentResponseDto> updatePostComment(
            @PathVariable("postId") Long postId, // Keep to avoid PathVariable mismatch error but we don't need it in service
            @PathVariable("commentId") Long commentId,
            @RequestBody CommentRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        validateContentType(postId, "feed");
        CommentResponseDto response = commentService.updateComment(commentId, requestDto, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/qna/{postId}/comments/{commentId}")
    public ResponseEntity<CommentResponseDto> updateQnaComment(
            @PathVariable("postId") Long postId, // Keep to avoid PathVariable mismatch error but we don't need it in service
            @PathVariable("commentId") Long commentId,
            @RequestBody CommentRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        validateContentType(postId, "qna");
        CommentResponseDto response = commentService.updateComment(commentId, requestDto, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    public ResponseEntity<Void> deletePostComment(
            @PathVariable("postId") Long postId,
            @PathVariable("commentId") Long commentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        validateContentType(postId, "feed");
        commentService.deleteComment(commentId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/qna/{postId}/comments/{commentId}")
    public ResponseEntity<Void> deleteQnaComment(
            @PathVariable("postId") Long postId,
            @PathVariable("commentId") Long commentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        validateContentType(postId, "qna");
        commentService.deleteComment(commentId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/posts/{postId}/comments/{commentId}/like")
    public ResponseEntity<Void> togglePostLike(
            @PathVariable("postId") Long postId,
            @PathVariable("commentId") Long commentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        validateContentType(postId, "feed");
        commentService.toggleInteraction(commentId, "like", userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/qna/{postId}/comments/{commentId}/like")
    public ResponseEntity<Void> toggleQnaLike(
            @PathVariable("postId") Long postId,
            @PathVariable("commentId") Long commentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        validateContentType(postId, "qna");
        commentService.toggleInteraction(commentId, "like", userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/posts/{postId}/comments/{commentId}/dislike")
    public ResponseEntity<Void> togglePostDislike(
            @PathVariable("postId") Long postId,
            @PathVariable("commentId") Long commentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        validateContentType(postId, "feed");
        commentService.toggleInteraction(commentId, "dislike", userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/qna/{postId}/comments/{commentId}/dislike")
    public ResponseEntity<Void> toggleQnaDislike(
            @PathVariable("postId") Long postId,
            @PathVariable("commentId") Long commentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        validateContentType(postId, "qna");
        commentService.toggleInteraction(commentId, "dislike", userDetails.getUsername());
        return ResponseEntity.ok().build();
    }
}
