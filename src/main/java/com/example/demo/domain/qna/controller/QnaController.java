package com.example.demo.domain.qna.controller;

import com.example.demo.domain.qna.dto.QnaCardResponseDto;
import com.example.demo.domain.qna.dto.QnaCreateRequestDto;
import com.example.demo.domain.qna.dto.QnaDetailResponseDto;
import com.example.demo.domain.qna.service.QnaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/qna")
@RequiredArgsConstructor
public class QnaController {

    private final QnaService qnaService;

    @PostMapping
    public ResponseEntity<?> createQna(@RequestBody QnaCreateRequestDto qnaCreateRequestDto,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }

        qnaService.createQna(qnaCreateRequestDto, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<Page<QnaCardResponseDto>> getQnaList(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails != null ? userDetails.getUsername() : null;
        Page<QnaCardResponseDto> qnaList = qnaService.getQnaList(q, sort, status, page, size, email);
        return ResponseEntity.ok(qnaList);
    }

    @GetMapping("/{qnaId}")
    public ResponseEntity<QnaDetailResponseDto> getQnaDetail(@PathVariable Long qnaId,
                                                              @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails != null ? userDetails.getUsername() : null;
        QnaDetailResponseDto qnaDetail = qnaService.getQnaDetail(qnaId, email);
        return ResponseEntity.ok(qnaDetail);
    }

    @PutMapping("/{qnaId}")
    public ResponseEntity<?> updateQna(@PathVariable Long qnaId,
                                       @RequestBody QnaCreateRequestDto qnaCreateRequestDto,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }

        qnaService.updateQna(qnaId, qnaCreateRequestDto, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{qnaId}")
    public ResponseEntity<?> deleteQna(@PathVariable Long qnaId,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }

        qnaService.deleteQna(qnaId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{qnaId}/like")
    public ResponseEntity<?> toggleLike(@PathVariable Long qnaId,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }

        qnaService.toggleLike(qnaId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{qnaId}/dislike")
    public ResponseEntity<?> toggleDislike(@PathVariable Long qnaId,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }

        qnaService.toggleDislike(qnaId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{qnaId}/bookmark")
    public ResponseEntity<?> toggleBookmark(@PathVariable Long qnaId,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }

        qnaService.toggleBookmark(qnaId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{qnaId}/comments/{commentId}/accept")
    public ResponseEntity<?> acceptAnswer(@PathVariable Long qnaId,
                                          @PathVariable Long commentId,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }

        qnaService.acceptAnswer(qnaId, commentId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }
}
