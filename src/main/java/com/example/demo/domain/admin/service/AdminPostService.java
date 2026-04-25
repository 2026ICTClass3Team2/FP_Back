package com.example.demo.domain.admin.service; // 🔴 이 경로가 실제 폴더 구조와 일치해야 합니다!

import com.example.demo.domain.admin.dto.AdminPostDto;
import com.example.demo.domain.admin.repository.AdminPostRepository;
import com.example.demo.domain.content.entity.Post;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class AdminPostService {

    private final AdminPostRepository adminPostRepository;

    public AdminPostService(AdminPostRepository adminPostRepository) {
        this.adminPostRepository = adminPostRepository;
    }

    @Transactional
    public Post incrementView(Long id) {
        Post post = adminPostRepository.findById(id).orElseThrow();
        int currentView = (post.getViewCount() == null) ? 0 : post.getViewCount();
        post.setViewCount(currentView + 1);
        return post;
    }

    public List<Post> findAll() {
        return adminPostRepository.findAllByContentTypeOrderByCreatedAtDesc("notice");
    }

    @Transactional
    public void save(AdminPostDto dto) {
        Post post = Post.builder()
                .title(dto.getTitle())
                .body(dto.getBody())
                .authorName("관리자")
                .contentType("notice")
                .sourceType("internal")
                .status("hidden") // 처음엔 비공개(HIDDEN) 저장 (이미 5개 공개상태에서 공개할려면 6개가 되니까 선택지도 줄 이유가 x)
                .viewCount(0)
                .build();
        adminPostRepository.save(post);
    }

    @Transactional
    public void update(Long id, AdminPostDto dto) {
        Post post = adminPostRepository.findById(id).orElseThrow();
        post.setTitle(dto.getTitle());
        post.setBody(dto.getBody());
    }

    @Transactional
    public void delete(Long id) {
        adminPostRepository.deleteById(id);
    }

    @Transactional
    public void toggleNoticeStatus(Long id) {
        Post post = adminPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("공지를 찾을 수 없습니다."));

        // VISIBLE <-> HIDDEN 토글
        String newStatus = "active".equals(post.getStatus()) ? "hidden" : "active";
        post.setStatus(newStatus);
    }
}