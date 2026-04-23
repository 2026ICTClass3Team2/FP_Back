package com.example.demo.domain.study.controller;

import com.example.demo.domain.report.entity.Hidden;
import com.example.demo.domain.report.enums.HiddenTargetType;
import com.example.demo.domain.report.repository.HiddenRepository;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/study")
@RequiredArgsConstructor
public class StudyController {

    private final HiddenRepository hiddenRepository;
    private final UserRepository   userRepository;

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

    /** 숨긴 언어 resource_id 목록 반환 — 관리자/유저 모두 호출 */
    @GetMapping("/hidden-languages")
    public ResponseEntity<List<Long>> getHiddenLanguages() {
        List<Long> ids = hiddenRepository.findTargetIdsByTargetType(HiddenTargetType.language);
        return ResponseEntity.ok(ids);
    }

    /** 언어 숨기기 (삭제) — 관리자 전용 */
    @PostMapping("/hidden-languages")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> hideLanguage(
            @RequestBody Map<String, Long> body,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long resourceId = body.get("resourceId");
        if (resourceId == null) return ResponseEntity.badRequest().build();

        if (!hiddenRepository.existsByTargetTypeAndTargetId(HiddenTargetType.language, resourceId)) {
            User admin = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            hiddenRepository.save(Hidden.builder()
                    .user(admin)
                    .targetId(resourceId)
                    .targetType(HiddenTargetType.language)
                    .build());
        }
        return ResponseEntity.ok().build();
    }

    /** 언어 복원 (숨김 해제) — 관리자 전용 */
    @DeleteMapping("/hidden-languages")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> restoreLanguage(@RequestBody Map<String, Long> body) {
        Long resourceId = body.get("resourceId");
        if (resourceId != null) {
            hiddenRepository.deleteByTargetTypeAndTargetId(HiddenTargetType.language, resourceId);
        }
        return ResponseEntity.ok().build();
    }
}
