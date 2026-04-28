package com.example.demo.domain.admin.controller;

import com.example.demo.domain.admin.dto.AdminPostDto;
import com.example.demo.domain.admin.service.AdminPostService;
import com.example.demo.domain.content.entity.Post;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/admin/notice") //  리액트와 주소 같게 하기
@RequiredArgsConstructor
@Slf4j

public class AdminPostController {

    private final AdminPostService adminPostService;

    @GetMapping("/list")
    public List<Post> list() {
        return adminPostService.findAll();
    }

    @PostMapping("/write")
    public ResponseEntity<?> writeNotice(@ModelAttribute AdminPostDto adminPostDto) {
        // 이제 adminPostDto.getFile()을 통해 업로드된 파일에 접근할 수 있습니다.
        log.info("파일 업로드 확인: {}", adminPostDto.getFile() != null);

        // 서비스 로직 실행...
        return ResponseEntity.ok("등록 성공");
    }

    // ✅ @RequestBody를 @ModelAttribute로 변경
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable(value = "id") Long id,
            @ModelAttribute AdminPostDto dto  // 🔴 수정됨
    ) {
        try {
            log.info("수정 요청 - ID: {}, 파일 존재 여부: {}", id, dto.getFile() != null);
            adminPostService.update(id, dto);
            return ResponseEntity.ok("SUCCESS");
        } catch (Exception e) {
            log.error("수정 중 오류 발생: ", e);
            return ResponseEntity.internalServerError().body("ERROR");
        }
    }

    @PatchMapping("/{id}/view")
    public ResponseEntity<?> updateViewCount(@PathVariable(value = "id") Long id) {
        try {
            return ResponseEntity.ok(adminPostService.incrementView(id).getViewCount());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("ERROR");
        }
    }

    // ERR_FAILED 해결: 토글 주소 명확화
    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<?> toggleStatus(@PathVariable(value = "id") Long id) {
        try {
            adminPostService.toggleNoticeStatus(id);
            return ResponseEntity.ok("SUCCESS");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("ERROR");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") Long id) {
        try {
            adminPostService.delete(id);
            return ResponseEntity.ok("SUCCESS");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("ERROR");
        }
    }
}