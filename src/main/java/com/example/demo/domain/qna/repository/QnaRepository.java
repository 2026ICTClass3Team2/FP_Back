package com.example.demo.domain.qna.repository;

import com.example.demo.domain.qna.entity.Qna;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QnaRepository extends JpaRepository<Qna, Long>, QnaRepositoryCustom {
    Qna findByPostId(Long postId);
}
