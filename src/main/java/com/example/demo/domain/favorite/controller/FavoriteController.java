package com.example.demo.domain.favorite.controller;

import com.example.demo.domain.content.dto.PostFeedResponseDto;
import com.example.demo.domain.favorite.dto.FavoriteUserDto;
import com.example.demo.domain.favorite.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    /** 즐겨찾기 토글 */
    @PostMapping("/users/{targetUserId}")
    public ResponseEntity<?> toggleFavorite(
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(401).build();
        try {
            boolean added = favoriteService.toggleFavorite(userDetails.getUsername(), targetUserId);
            return ResponseEntity.ok(Map.of("favorited", added));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** 즐겨찾기 유저 목록 */
    @GetMapping("/users")
    public ResponseEntity<List<FavoriteUserDto>> getFavoriteUsers(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(favoriteService.getFavoriteUsers(userDetails.getUsername()));
    }

    /** 즐겨찾기 유저들의 게시글 피드 */
    @GetMapping("/feed")
    public ResponseEntity<Slice<PostFeedResponseDto>> getFavoritesFeed(
            @RequestParam(required = false) Long lastPostId,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(favoriteService.getFavoritesFeed(userDetails.getUsername(), lastPostId, size));
    }
}
