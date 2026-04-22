package com.example.demo.domain.admin.service; // 🔴 이 주소가 폴더 구조와 똑같아야 합니다!

import com.example.demo.domain.admin.dto.AdminPostDto;
import com.example.demo.domain.admin.repository.AdminPostRepository;
import com.example.demo.domain.content.entity.Post;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class AdminPostService {

    private final AdminPostRepository adminPostRepository;

    // 🔴 @Qualifier 안에 방금 만드신 "adminNoticeRepo"를 정확히 적어주세요.
    public AdminPostService(@Qualifier("adminNoticeRepo") AdminPostRepository adminPostRepository) {
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
                .status("active")
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
}