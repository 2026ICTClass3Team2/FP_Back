package com.example.demo.domain.admin.service;

import com.example.demo.domain.admin.dto.AdminPostDto;
import com.example.demo.domain.admin.repository.AdminPostRepository;
import com.example.demo.domain.content.entity.Post;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Service
public class AdminPostService {

    private final AdminPostRepository adminPostRepository;
    private final String uploadDir = "./upload-dir";

    public AdminPostService(AdminPostRepository adminPostRepository) {
        this.adminPostRepository = adminPostRepository;
    }

    @Transactional
    public void save(AdminPostDto dto) {
        String savedFileName = null;
        // 🔴 MultipartFile 필드명이 DTO와 일치하는지 확인 필수
        if (dto.getFile() != null && !dto.getFile().isEmpty()) {
            savedFileName = saveFileToDisk(dto.getFile());
        }

        Post post = Post.builder()
                .title(dto.getTitle()).body(dto.getBody()).tag(dto.getTag())
                .authorName("관리자").contentType("notice").sourceType("internal")
                .status(dto.isVisible() ? "active" : "hidden").viewCount(0)
                .fileName(savedFileName).fileUrl(dto.getFileUrl()).build();
        adminPostRepository.save(post);
    }

    @Transactional
    public void update(Long id, AdminPostDto dto) {
        Post post = adminPostRepository.findById(id).orElseThrow();
        post.setTitle(dto.getTitle());
        post.setBody(dto.getBody());
        post.setTag(dto.getTag());
        post.setStatus(dto.isVisible() ? "active" : "hidden");
        post.setFileUrl(dto.getFileUrl());

        if (dto.getFile() != null && !dto.getFile().isEmpty()) {
            post.setFileName(saveFileToDisk(dto.getFile()));
        }
    }

    private String saveFileToDisk(MultipartFile file) {
        try {
            Path root = Paths.get(uploadDir);
            if (!Files.exists(root)) Files.createDirectories(root);

            // 🔴 UUID를 사용하여 파일명 충돌 및 특수문자 500 에러 방지
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String cleanFileName = UUID.randomUUID().toString() + extension;

            Path filePath = root.resolve(cleanFileName);
            file.transferTo(filePath.toFile());
            return cleanFileName;
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 중 서버 내부 오류 발생: " + e.getMessage());
        }
    }

    public List<Post> findAll() { return adminPostRepository.findAllByContentTypeOrderByCreatedAtDesc("notice"); }
    @Transactional public void delete(Long id) { adminPostRepository.deleteById(id); }
    @Transactional public Post incrementView(Long id) {
        Post post = adminPostRepository.findById(id).orElseThrow();
        post.setViewCount((post.getViewCount() == null ? 0 : post.getViewCount()) + 1);
        return post;
    }

    @Transactional
    public void toggleNoticeStatus(Long id) {
        Post post = adminPostRepository.findById(id).orElseThrow();
        post.setStatus("active".equals(post.getStatus()) ? "hidden" : "active");
    }
}