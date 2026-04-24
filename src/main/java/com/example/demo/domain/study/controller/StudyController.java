package com.example.demo.domain.study.controller;

import com.example.demo.domain.study.dto.HiddenChapDetailDto;
import com.example.demo.domain.study.dto.HiddenLangDetailDto;
import com.example.demo.domain.study.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/study")
@RequiredArgsConstructor
public class StudyController {

    private final ResourceRepository resourceRepository;

    /** 현재 사용자의 역할 반환 */
    @GetMapping("/my-role")
    public ResponseEntity<Map<String, String>> getMyRole(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) return ResponseEntity.status(401).build();

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        return ResponseEntity.ok(Map.of("role", isAdmin ? "admin" : "user"));
    }

    /** 관리자 권한 확인 */
    @GetMapping("/admin-check")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> adminCheck() {
        return ResponseEntity.ok(Map.of("authorized", true));
    }

    /** is_hidden = true 인 resource_id 목록 반환 — 관리자/유저 모두 호출 */
    @GetMapping("/hidden-languages")
    public ResponseEntity<List<Long>> getHiddenLanguages() {
        return ResponseEntity.ok(resourceRepository.findHiddenResourceIds());
    }

    /** 언어 숨기기 (resource, original, translated 모두 is_hidden = true) — 관리자 전용 */
    @PostMapping("/hidden-languages")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> hideLanguage(@RequestBody Map<String, Long> body) {
        Long resourceId = body.get("resourceId");
        if (resourceId == null) return ResponseEntity.badRequest().build();

        resourceRepository.updateResourceIsHidden(resourceId, true);
        resourceRepository.updateOriginalIsHidden(resourceId, true);
        resourceRepository.updateTranslatedIsHidden(resourceId, true);
        return ResponseEntity.ok().build();
    }

    /** 언어 복원 (resource, original, translated 모두 is_hidden = false) — 관리자 전용 */
    @DeleteMapping("/hidden-languages")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> restoreLanguage(@RequestBody Map<String, Long> body) {
        Long resourceId = body.get("resourceId");
        if (resourceId == null) return ResponseEntity.badRequest().build();

        resourceRepository.updateResourceIsHidden(resourceId, false);
        resourceRepository.updateOriginalIsHidden(resourceId, false);
        resourceRepository.updateTranslatedIsHidden(resourceId, false);
        return ResponseEntity.ok().build();
    }

    // ── 챕터 숨김/복원 ─────────────────────────────────────────────────────────

    /** 숨긴 챕터 original_id 목록 — 관리자/유저 모두 호출 */
    @GetMapping("/hidden-chapters")
    public ResponseEntity<List<Long>> getHiddenChapters() {
        return ResponseEntity.ok(resourceRepository.findHiddenOriginalIds());
    }

    /** 챕터 숨기기 — 관리자 전용 */
    @PostMapping("/hidden-chapters")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> hideChapter(@RequestBody Map<String, Long> body) {
        Long originalId = body.get("originalId");
        if (originalId == null) return ResponseEntity.badRequest().build();
        resourceRepository.hideOriginal(originalId);
        return ResponseEntity.ok().build();
    }

    /** 챕터 복원 — 관리자 전용 */
    @DeleteMapping("/hidden-chapters")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> restoreChapter(@RequestBody Map<String, Long> body) {
        Long originalId = body.get("originalId");
        if (originalId == null) return ResponseEntity.badRequest().build();
        resourceRepository.restoreOriginal(originalId);
        return ResponseEntity.ok().build();
    }

    // ── 삭제한 목록 상세 조회 ──────────────────────────────────────────────────

    /** 삭제한 언어 상세 목록 — 관리자 전용 */
    @GetMapping("/hidden-languages-detail")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<HiddenLangDetailDto>> getHiddenLanguagesDetail() {
        List<HiddenLangDetailDto> result = resourceRepository.findHiddenLangsDetail()
                .stream()
                .map(r -> new HiddenLangDetailDto(r.getResourceId(), r.getName(), r.getFirstChapterContent(), null))
                .toList();
        return ResponseEntity.ok(result);
    }

    /** 삭제한 챕터 상세 목록 — 관리자 전용 */
    @GetMapping("/hidden-chapters-detail")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<HiddenChapDetailDto>> getHiddenChaptersDetail() {
        List<HiddenChapDetailDto> result = resourceRepository.findHiddenChapsDetail()
                .stream()
                .map(r -> new HiddenChapDetailDto(r.getOriginalId(), r.getLanguageName(), r.getTitle(), r.getFirstContent(), null))
                .toList();
        return ResponseEntity.ok(result);
    }

    // ── 수정 ───────────────────────────────────────────────────────────────────

    /** 언어 이름 수정 — 관리자 전용 */
    @PatchMapping("/resource/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> updateResourceName(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank()) return ResponseEntity.badRequest().build();
        resourceRepository.updateResourceName(id, name.trim());
        return ResponseEntity.ok().build();
    }

    /**
     * 챕터 수정 — 관리자 전용
     * translated(ko) 레코드가 있으면 우선 수정, 없으면 original 수정
     */
    @PatchMapping("/chapter/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> updateChapter(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String title   = body.get("title");
        String content = body.get("content");
        if (title == null || content == null) return ResponseEntity.badRequest().build();
        int updated = resourceRepository.updateTranslatedChapter(id, title, content);
        if (updated == 0) {
            resourceRepository.updateOriginalChapter(id, title, content);
        }
        return ResponseEntity.ok().build();
    }
}
