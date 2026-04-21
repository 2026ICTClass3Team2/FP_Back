package com.example.demo.domain.admin.service;

import com.example.demo.domain.admin.dto.AdminPostDto;
import com.example.demo.domain.admin.repository.AdminPostRepository;
import com.example.demo.domain.content.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPostService {

    private final AdminPostRepository adminPostRepository;

    // 등록
    public void save(AdminPostDto dto) {

        Post post = Post.builder()
                .title(dto.getTitle())
                .body(dto.getBody())
                .contentType("notice")
                .sourceType("internal")
                .status("active")
                .isHidden(false)
                .isSolved(false)
                .build();

        adminPostRepository.save(post);
    }

    // 목록 조회
    public List<Post> findAll() {
        return adminPostRepository
                .findAllByContentTypeOrderByCreatedAtDesc("notice");
    }

    // 수정
    public void update(Long id, AdminPostDto dto) {

        Post post = adminPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("게시글 없음"));

        post.setTitle(dto.getTitle());
        post.setBody(dto.getBody());

        adminPostRepository.save(post);
    }

    // 삭제
    public void delete(Long id) {
        adminPostRepository.deleteById(id);
    }
}