package com.example.demo.domain.admin.service;

import com.example.demo.domain.admin.dto.AdminPostDto;
import com.example.demo.domain.admin.repository.AdminPostRepository;
import com.example.demo.domain.content.entity.Post;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

@Slf4j
@Service
public class AdminPostService {

    private final AdminPostRepository adminPostRepository;
    // 경로를 절대 경로로 깔끔하게 관리하기 위해 상수로 선언
    private final Path rootLocation = Paths.get("upload-dir").toAbsolutePath().normalize();

    public AdminPostService(AdminPostRepository adminPostRepository) {
        this.adminPostRepository = adminPostRepository;
        // 서비스 시작 시 폴더가 없으면 생성
        try {
            if (!Files.exists(rootLocation)) {
                Files.createDirectories(rootLocation);
            }
        } catch (IOException e) {
            log.error("Could not initialize storage", e);
        }
    }

    @Transactional
    public void save(AdminPostDto dto) {
        String savedFileName = null;
        if (dto.getFile() != null && !dto.getFile().isEmpty()) {
            savedFileName = saveFileToDisk(dto.getFile());
        }

        Post post = Post.builder()
                .title(dto.getTitle()) // 사용자가 입력한 제목 그대로 저장
                .body(dto.getBody())
                .tag(dto.getTag())
                .authorName("관리자")
                .contentType("notice")
                .sourceType("internal")
                .status(dto.isVisible() ? "active" : "hidden")
                .viewCount(0)
                .fileName(savedFileName) // 원본 파일명이 저장됨
                .fileUrl(dto.getFileUrl())
                .build();
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
            // 🔴 UUID를 제거하고 원본 파일명을 사용합니다.
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) return null;

            Path targetLocation = rootLocation.resolve(originalFilename);

            // 동일 파일명 존재 시 덮어쓰기 (중복 방지가 필요하면 다시 UUID를 써야 하지만 제목 깨짐 방지를 위해 우선 원본 유지)
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("File stored successfully: {}", originalFilename);
            return originalFilename;
        } catch (IOException e) {
            log.error("Could not store file. Error: {}", e.getMessage());
            throw new RuntimeException("파일 저장 실패!", e);
        }
    }

    // findAll 메서드를 아래와 같이 수정하세요.
    public List<Post> findAll() {
        // 🔴 다시 전체를 다 가져오도록 복구합니다. (상태값 필터링 제거)
        return adminPostRepository.findAllByContentTypeOrderByCreatedAtDesc("notice");
    }
    @Transactional
    public void delete(Long id) {
        adminPostRepository.deleteById(id);
    }

    @Transactional
    public Post incrementView(Long id) {
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