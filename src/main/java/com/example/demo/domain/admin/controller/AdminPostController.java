package com.example.demo.domain.admin.controller;

import com.example.demo.domain.admin.dto.AdminPostDto;
import com.example.demo.domain.admin.service.AdminPostService;
import com.example.demo.domain.content.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/notice")
@RequiredArgsConstructor
public class AdminPostController {

    private final AdminPostService adminPostService;

    // 등록
    @PostMapping
    public String create(@RequestBody AdminPostDto dto) {
        adminPostService.save(dto);
        return "등록 완료";
    }

    // 목록 조회
    @GetMapping("/list")
    public List<Post> list() {
        return adminPostService.findAll();
    }

    // 수정
    @PutMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @RequestBody AdminPostDto dto
    ) {
        adminPostService.update(id, dto);
        return "수정 완료";
    }

    // 삭제
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        adminPostService.delete(id);
        return "삭제 완료";
    }
}