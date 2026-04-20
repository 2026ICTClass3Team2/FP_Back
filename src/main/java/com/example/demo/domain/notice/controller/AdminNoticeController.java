package com.example.demo.domain.notice.controller;

import com.example.demo.domain.content.dto.PostCreateRequestDto;
import com.example.demo.domain.content.entity.Post;
import com.example.demo.domain.notice.repository.AdminNoticeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/notice")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminNoticeController {

    @Autowired
    private AdminNoticeRepository postRepository;

    // 1. 공지사항 목록 조회
    @GetMapping("/list")
    public ResponseEntity<List<Post>> getAdminNotices() {
        List<Post> notices = postRepository.findAll().stream()
                .filter(p -> "notice".equals(p.getContentType()))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
        return ResponseEntity.ok(notices);
    }

    // 2. 조회수 증가
    @PatchMapping("/view/{id}")
    public void incrementViewCount(@PathVariable Long id) {
        Post post = postRepository.findById(id).orElseThrow();
        Integer currentView = post.getViewCount();
        post.setViewCount((currentView == null ? 0 : currentView) + 1);
        postRepository.save(post);
    }

    // 3. 공지사항 등록
    @PostMapping("/write")
    public ResponseEntity<?> writeAdminNotice(@RequestBody PostCreateRequestDto request) {
        try {
            Post post = Post.builder()
                    .title(request.getTitle())
                    .body(request.getBody())
                    .authorName("관리자")
                    .contentType("notice")
                    .sourceType("internal")
                    .status("active")
                    .isHidden(false)
                    .isSolved(false)
                    .commentCount(0)
                    .likeCount(0)
                    .dislikeCount(0)
                    .viewCount(0)
                    .build();

            postRepository.save(post);
            return ResponseEntity.ok("관리자 공지사항이 성공적으로 등록되었습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("등록 실패: " + e.getMessage());
        }
    }

    // 4. 공지 삭제 (DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotice(@PathVariable Long id) {
        postRepository.deleteById(id);
        return ResponseEntity.ok("삭제 완료");
    }

    // 5. 공지 수정 (PUT)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateNotice(@PathVariable Long id, @RequestBody PostCreateRequestDto request) {
        try {
            Post post = postRepository.findById(id).orElseThrow();

            // 🔴 수정 포인트: getPostId() 대신 getId() 사용 시도
            // 만약 엔티티 필드명이 postId라면 그대로 두시고, id라면 getId()로 바꾸세요.
            Post updatedPost = Post.builder()
                    .id(post.getId()) // 👈 이 부분을 엔티티 필드명에 맞춰 'id'로 수정했습니다.
                    .title(request.getTitle())
                    .body(request.getBody())
                    .authorName(post.getAuthorName())
                    .contentType(post.getContentType())
                    .createdAt(post.getCreatedAt())
                    .viewCount(post.getViewCount())
                    .status(post.getStatus())
                    .sourceType(post.getSourceType())
                    .isHidden(post.getIsHidden())
                    .build();

            postRepository.save(updatedPost);
            return ResponseEntity.ok("수정 완료");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("수정 실패: " + e.getMessage());
        }
    }
}