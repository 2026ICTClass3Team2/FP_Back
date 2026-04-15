package com.example.demo.domain.qna.controller;

import com.example.demo.domain.qna.dto.QnaCardResponseDto;
import com.example.demo.domain.qna.dto.QnaCreateRequestDto;
import com.example.demo.domain.qna.dto.QnaDetailResponseDto;
import com.example.demo.domain.qna.service.QnaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/qna")
@RequiredArgsConstructor
public class QnaController {

    private final QnaService qnaService;

    @PostMapping
    public ResponseEntity<Void> createQna(@RequestBody QnaCreateRequestDto qnaCreateRequestDto, Principal principal) {
        qnaService.createQna(qnaCreateRequestDto, principal.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<Page<QnaCardResponseDto>> getQnaList(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Page<QnaCardResponseDto> qnaList = qnaService.getQnaList(q, sort, status, page, size);
        return ResponseEntity.ok(qnaList);
    }

    @GetMapping("/{qnaId}")
    public ResponseEntity<QnaDetailResponseDto> getQnaDetail(@PathVariable Long qnaId) {
        QnaDetailResponseDto qnaDetail = qnaService.getQnaDetail(qnaId);
        return ResponseEntity.ok(qnaDetail);
    }
}
