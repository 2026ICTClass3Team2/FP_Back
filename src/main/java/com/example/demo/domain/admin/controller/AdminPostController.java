package com.example.demo.domain.admin.controller;

import com.example.demo.domain.admin.dto.AdminPostDto;
import com.example.demo.domain.admin.service.AdminPostService;
import com.example.demo.domain.content.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/admin/notice")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
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

    @PatchMapping("/{id}/view")
    public ResponseEntity<?> updateViewCount(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(adminPostService.incrementView(id).getViewCount());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("ERROR");
        }
    }
}