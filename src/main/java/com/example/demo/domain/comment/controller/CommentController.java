package com.example.demo.domain.comment.controller;

import com.example.demo.domain.comment.dto.CommentRequestDto;
import com.example.demo.domain.comment.dto.CommentResponseDto;
import com.example.demo.domain.comment.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentResponseDto> createComment(
            @PathVariable Long postId,
            @RequestBody CommentRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        CommentResponseDto response = commentService.createComment(postId, requestDto, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<CommentResponseDto>> getComments(@PathVariable Long postId) {
        List<CommentResponseDto> responses = commentService.getComments(postId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponseDto> updateComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody CommentRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        CommentResponseDto response = commentService.updateComment(commentId, requestDto, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        commentService.deleteComment(commentId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{commentId}/like")
    public ResponseEntity<Void> toggleLike(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        commentService.toggleInteraction(commentId, "like", userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{commentId}/dislike")
    public ResponseEntity<Void> toggleDislike(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        commentService.toggleInteraction(commentId, "dislike", userDetails.getUsername());
        return ResponseEntity.ok().build();
    }
}
