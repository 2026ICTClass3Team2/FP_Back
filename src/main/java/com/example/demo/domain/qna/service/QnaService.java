package com.example.demo.domain.qna.service;

import com.example.demo.domain.qna.dto.QnaCardResponseDto;
import com.example.demo.domain.qna.dto.QnaCreateRequestDto;
import com.example.demo.domain.qna.dto.QnaDetailResponseDto;
import org.springframework.data.domain.Page;

public interface QnaService {
    void createQna(QnaCreateRequestDto qnaCreateRequestDto, String email);
    void updateQna(Long qnaId, QnaCreateRequestDto qnaCreateRequestDto, String email);
    Page<QnaCardResponseDto> getQnaList(String query, String sort, String status, int page, int size, String email);
    QnaDetailResponseDto getQnaDetail(Long qnaId, String email);
    Long resolveQnaPostId(Long qnaIdentifier);
    void deleteQna(Long qnaId, String email);
    void acceptAnswer(Long qnaId, Long commentId, String email);
    void toggleLike(Long qnaId, String email);
    void toggleDislike(Long qnaId, String email);
    void toggleBookmark(Long qnaId, String email);
}
