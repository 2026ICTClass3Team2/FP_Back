package com.example.demo.domain.shop.controller;

import com.example.demo.domain.shop.dto.EmoteResponseDto;
import com.example.demo.domain.shop.dto.EmoteUploadRequestDto;
import com.example.demo.domain.shop.dto.PurchaseHistoryDto;
import com.example.demo.domain.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    /** 이모티콘 목록 조회 (정렬 / 필터 지원) */
    @GetMapping("/emotes")
    public ResponseEntity<Page<EmoteResponseDto>> getEmotes(
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "false") boolean purchasedOnly,
            @RequestParam(defaultValue = "false") boolean unpurchasedOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails != null ? userDetails.getUsername() : null;
        Page<EmoteResponseDto> result = shopService.getEmotes(sort, purchasedOnly, unpurchasedOnly, page, size, email);
        return ResponseEntity.ok(result);
    }

    /** 관리자 이모티콘 등록 */
    @PostMapping("/emotes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmoteResponseDto> uploadEmote(
            @RequestBody EmoteUploadRequestDto dto) {
        return ResponseEntity.ok(shopService.uploadEmote(dto));
    }

    /** 관리자 이모티콘 수정 */
    @PutMapping("/emotes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmoteResponseDto> updateEmote(
            @PathVariable Long id,
            @RequestBody EmoteUploadRequestDto dto) {
        return ResponseEntity.ok(shopService.updateEmote(id, dto));
    }

    /** 이모티콘 구매 */
    @PostMapping("/emotes/{emoteId}/purchase")
    public ResponseEntity<Map<String, Object>> purchaseEmote(
            @PathVariable Long emoteId,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            Map<String, Object> result = shopService.purchaseEmote(emoteId, userDetails.getUsername());
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** 내 구매 내역 */
    @GetMapping("/purchase-history")
    public ResponseEntity<Page<PurchaseHistoryDto>> getPurchaseHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(shopService.getPurchaseHistory(userDetails.getUsername(), page, size));
    }

    /** 내 현재 포인트 조회 */
    @GetMapping("/my-points")
    public ResponseEntity<Map<String, Object>> getMyPoints(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        int points = shopService.getUserPoints(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("points", points));
    }

    /**
     * 포인트 내역 - 포인트 시스템 구현 후 PointService와 연결 예정
     * 현재는 빈 페이지 반환
     */
    @GetMapping("/point-history")
    public ResponseEntity<Map<String, Object>> getPointHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(Map.of(
                "content", List.of(),
                "totalPages", 0,
                "totalElements", 0,
                "number", page
        ));
    }
}
