package com.example.demo.domain.admin.controller;

import com.example.demo.domain.admin.dto.AdminPostDto;
import com.example.demo.domain.admin.service.AdminPostService;
import com.example.demo.domain.content.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/admin/notice") //  리액트와 주소 같게 하기
@RequiredArgsConstructor

public class AdminPostController {

    private final AdminPostService adminPostService;

    @GetMapping("/list")
    public List<Post> list() {
        return adminPostService.findAll();
    }

    @PostMapping("/write")
    public String create(@RequestBody AdminPostDto dto) {
        adminPostService.save(dto);
        return "SUCCESS";
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable(value = "id") Long id, @RequestBody AdminPostDto dto) {
        try {
            adminPostService.update(id, dto);
            return ResponseEntity.ok("SUCCESS");
        } catch (Exception e) {
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