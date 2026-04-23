package com.example.demo.domain.mypage.controller;

import com.example.demo.domain.mypage.dto.BlockResponseDto;
import com.example.demo.domain.mypage.dto.EmailAuthRequestDto;
import com.example.demo.domain.mypage.dto.EmailVerifyRequestDto;
import com.example.demo.domain.mypage.dto.MyPageProfileResponseDto;
import com.example.demo.domain.mypage.dto.MyPostDto;
import com.example.demo.domain.mypage.dto.PasswordUpdateRequestDto;
import com.example.demo.domain.mypage.dto.ProfileUpdateRequestDto;
import com.example.demo.domain.mypage.service.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    @GetMapping("/profile")
    public ResponseEntity<MyPageProfileResponseDto> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        MyPageProfileResponseDto profile = myPageService.getProfile(userDetails.getUsername());
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestBody ProfileUpdateRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            myPageService.updateProfile(userDetails.getUsername(), requestDto);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/password")
    public ResponseEntity<?> updatePassword(
            @RequestBody PasswordUpdateRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            myPageService.updatePassword(userDetails.getUsername(), requestDto);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/email/request")
    public ResponseEntity<?> requestEmailVerification(
            @RequestBody EmailAuthRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            myPageService.requestEmailVerification(requestDto.getEmail());
            return ResponseEntity.ok(Map.of("message", "인증번호가 발송되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "메일 발송 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/email/verify")
    public ResponseEntity<?> verifyEmailAndChange(
            @RequestBody EmailVerifyRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            myPageService.verifyEmailAndChange(userDetails.getUsername(), requestDto.getEmail(), requestDto.getCode());
            return ResponseEntity.ok(Map.of("message", "이메일이 변경되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "e.getMessage()"));
        }
    }

    @GetMapping("/posts")
    public ResponseEntity<Page<MyPostDto>> getMyPosts(
            @RequestParam(defaultValue = "all") String category,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        Page<MyPostDto> posts = myPageService.getMyPosts(userDetails.getUsername(), category, sort, page, size);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/bookmarks")
    public ResponseEntity<Page<MyPostDto>> getMyBookmarks(
            @RequestParam(defaultValue = "all") String category,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        Page<MyPostDto> bookmarks = myPageService.getMyBookmarks(userDetails.getUsername(), category, sort, page, size);
        return ResponseEntity.ok(bookmarks);
    }

    @GetMapping("/blocks")
    public ResponseEntity<Page<BlockResponseDto>> getBlockedUsers(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        Page<BlockResponseDto> blockedUsers = myPageService.getBlockedUsers(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(blockedUsers);
    }

    @DeleteMapping("/blocks/{blockId}")
    public ResponseEntity<?> unblockUser(
            @PathVariable Long blockId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        myPageService.unblockUser(userDetails.getUsername(), blockId);
        return ResponseEntity.ok(Map.of("message", "차단이 성공적으로 해제되었습니다."));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<MyPageProfileResponseDto> getUserProfile(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String viewerEmail = userDetails != null ? userDetails.getUsername() : null;
        MyPageProfileResponseDto profile = myPageService.getUserProfileById(userId, viewerEmail);
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/users/{userId}/block")
    public ResponseEntity<?> blockUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(401).build();
        try {
            myPageService.blockUserDirectly(userDetails.getUsername(), userId);
            return ResponseEntity.ok(Map.of("message", "차단되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/users/{userId}/block")
    public ResponseEntity<?> unblockUserByTarget(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(401).build();
        myPageService.unblockUserByTargetId(userDetails.getUsername(), userId);
        return ResponseEntity.ok(Map.of("message", "차단이 해제되었습니다."));
    }
}
