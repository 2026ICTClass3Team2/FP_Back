package com.example.demo.domain.study.controller;

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
}
