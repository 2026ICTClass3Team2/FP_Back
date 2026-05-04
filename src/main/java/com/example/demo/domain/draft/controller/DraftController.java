package com.example.demo.domain.draft.controller;

import com.example.demo.domain.draft.dto.DraftResponseDto;
import com.example.demo.domain.draft.dto.DraftSaveRequestDto;
import com.example.demo.domain.draft.service.DraftService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/drafts")
public class DraftController {

    private final DraftService draftService;

    @PostMapping
    public ResponseEntity<?> createDraft(
            @RequestBody DraftSaveRequestDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        Long draftId = draftService.createDraft(dto, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("draftId", draftId));
    }

    @PutMapping("/{draftId}")
    public ResponseEntity<?> updateDraft(
            @PathVariable Long draftId,
            @RequestBody DraftSaveRequestDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        try {
            draftService.updateDraft(draftId, dto, userDetails.getUsername());
            return ResponseEntity.ok(Map.of("message", "임시저장이 업데이트되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> listDrafts(
            @RequestParam String type,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        List<DraftResponseDto> drafts = draftService.listDrafts(userDetails.getUsername(), type);
        return ResponseEntity.ok(drafts);
    }

    @GetMapping("/{draftId}")
    public ResponseEntity<?> getDraft(
            @PathVariable Long draftId,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        try {
            return ResponseEntity.ok(draftService.getDraft(draftId, userDetails.getUsername()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{draftId}")
    public ResponseEntity<?> deleteDraft(
            @PathVariable Long draftId,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        try {
            draftService.deleteDraft(draftId, userDetails.getUsername());
            return ResponseEntity.ok(Map.of("message", "임시저장이 삭제되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }
}
