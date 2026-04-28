package com.example.demo.domain.admin.controller;

import com.example.demo.domain.admin.dto.AdminPostDto;
import com.example.demo.domain.admin.service.AdminPostService;
import com.example.demo.domain.content.entity.Post;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/admin/notice")
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
        try {
            adminPostService.save(adminPostDto);
            return ResponseEntity.ok("SUCCESS");
        } catch (Exception e) {
            log.error("등록 에러", e);
            return ResponseEntity.internalServerError().body("FAIL");
        }
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get("./upload-dir").resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                String encodedFileName = UriUtils.encode(fileName, StandardCharsets.UTF_8);

                // 🔴 핵심: attachment 설정으로 브라우저의 '열기' 동작 방지 및 즉시 저장 유도
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName)
                        .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Download Error", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable(value = "id") Long id, @ModelAttribute AdminPostDto dto) {
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