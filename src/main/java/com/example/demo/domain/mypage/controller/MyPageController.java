package com.example.demo.domain.mypage.controller;

import com.example.demo.domain.mypage.dto.EmailAuthRequestDto;
import com.example.demo.domain.mypage.dto.EmailVerifyRequestDto;
import com.example.demo.domain.mypage.dto.MyPageProfileResponseDto;
import com.example.demo.domain.mypage.dto.PasswordUpdateRequestDto;
import com.example.demo.domain.mypage.dto.ProfileUpdateRequestDto;
import com.example.demo.domain.mypage.service.MyPageService;
import lombok.RequiredArgsConstructor;
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
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
