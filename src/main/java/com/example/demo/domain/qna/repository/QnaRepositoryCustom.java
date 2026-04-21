package com.example.demo.domain.qna.repository;

import com.example.demo.domain.qna.dto.QnaCardResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QnaRepositoryCustom {
    Page<QnaCardResponseDto> findQnaList(String query, String sort, String status, Pageable pageable);
}
