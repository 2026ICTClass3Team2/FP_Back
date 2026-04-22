package com.example.demo.domain.admin.controller;

import com.example.demo.domain.admin.dto.AdminDashboardStatsDto;
import com.example.demo.domain.admin.dto.AdminUserDto;
import com.example.demo.domain.admin.dto.AdminChannelDto;
import com.example.demo.domain.admin.dto.ReportAdminDto;
import com.example.demo.domain.admin.dto.SuspendRequestDto;
import com.example.demo.domain.admin.service.AdminService;
import com.example.demo.domain.suggestion.entity.Suggestion;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
// @PreAuthorize("hasRole('ADMIN')") // Enable if Roles are setup properly with 'ROLE_ADMIN'
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    public ResponseEntity<AdminDashboardStatsDto> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserDto>> searchUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminService.searchUsers(keyword, status, pageable));
    }

    @PostMapping("/users/{userId}/warn")
    public ResponseEntity<?> warnUser(@PathVariable Long userId, @AuthenticationPrincipal UserDetails userDetails) {
        // Assume userDetails has some way to get admin ID. For now we fetch by email/username in service or just pass placeholder.
        // Actually we need the admin's ID. Let's assume we can get it or we just use 1L as a fallback.
        // In a real app we'd fetch the admin User entity. We'll leave the adminId retrieval abstraction here.
        adminService.warnUser(userId, 1L); // TODO: Replace 1L with actual admin ID
        return ResponseEntity.ok(Map.of("message", "User warned successfully"));
    }

    @PostMapping("/users/{userId}/suspend")
    public ResponseEntity<?> suspendUser(@PathVariable Long userId, @RequestBody SuspendRequestDto requestDto) {
        adminService.suspendUser(userId, 1L, requestDto); // TODO: Replace 1L with actual admin ID
        return ResponseEntity.ok(Map.of("message", "User suspended successfully"));
    }

    @PostMapping("/users/{userId}/revert-warn")
    public ResponseEntity<?> revertWarnUser(@PathVariable Long userId, @RequestBody(required = false) Map<String, String> body) {
        String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "I made a mistake";
        adminService.revertWarnUser(userId, 1L, reason);
        return ResponseEntity.ok(Map.of("message", "Warning reverted successfully"));
    }

    @PostMapping("/users/{userId}/revert-suspend")
    public ResponseEntity<?> revertSuspendUser(@PathVariable Long userId, @RequestBody(required = false) Map<String, String> body) {
        String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "I made a mistake";
        adminService.revertSuspendUser(userId, 1L, reason);
        return ResponseEntity.ok(Map.of("message", "Suspension reverted successfully"));
    }

    @PutMapping("/users/{userId}/status")
    public ResponseEntity<?> updateUserStatus(@PathVariable Long userId, @RequestParam String status) {
        adminService.updateUserStatus(userId, status);
        return ResponseEntity.ok(Map.of("message", "User status updated"));
    }

    @GetMapping("/reports")
    public ResponseEntity<Page<ReportAdminDto>> getReports(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminService.getReports(status, pageable));
    }

    @PutMapping("/reports/{reportId}/status")
    public ResponseEntity<?> updateReportStatus(@PathVariable Long reportId, @RequestParam String status) {
        adminService.updateReportStatus(reportId, status);
        return ResponseEntity.ok(Map.of("message", "Report status updated"));
    }

    @GetMapping("/reports/{reportId}/target")
    public ResponseEntity<?> getReportTargetDetails(@PathVariable Long reportId) {
        return ResponseEntity.ok(adminService.getReportTargetDetails(reportId));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId) {
        adminService.deleteComment(commentId);
        return ResponseEntity.ok(Map.of("message", "Comment deleted"));
    }

    @PutMapping("/comments/{commentId}/status")
    public ResponseEntity<?> updateCommentStatus(@PathVariable Long commentId, @RequestParam String status) {
        adminService.updateCommentStatus(commentId, status);
        return ResponseEntity.ok(Map.of("message", "Comment status updated"));
    }

    @PutMapping("/posts/{postId}/hide")
    public ResponseEntity<?> hidePost(@PathVariable Long postId) {
        adminService.hidePost(postId);
        return ResponseEntity.ok(Map.of("message", "Post hidden"));
    }

    @PutMapping("/posts/{postId}/status")
    public ResponseEntity<?> updatePostStatus(@PathVariable Long postId, @RequestParam String status) {
        adminService.updatePostStatus(postId, status);
        return ResponseEntity.ok(Map.of("message", "Post status updated"));
    }

    @PutMapping("/channels/{channelId}/hide")
    public ResponseEntity<?> hideChannel(@PathVariable Long channelId) {
        adminService.hideChannel(channelId);
        return ResponseEntity.ok(Map.of("message", "Channel hidden"));
    }

    @GetMapping("/channels")
    public ResponseEntity<Page<AdminChannelDto>> searchChannels(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminService.searchChannels(keyword, status, pageable));
    }

    @PutMapping("/channels/{channelId}/status")
    public ResponseEntity<?> updateChannelStatus(@PathVariable Long channelId, @RequestParam String status) {
        adminService.updateChannelStatus(channelId, status);
        return ResponseEntity.ok(Map.of("message", "Channel status updated"));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<Page<Suggestion>> getSuggestions(
            @RequestParam(required = false) Boolean isSeen,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminService.getSuggestions(isSeen, pageable));
    }

    @PutMapping("/suggestions/{suggestionId}/seen")
    public ResponseEntity<?> markSuggestionAsSeen(@PathVariable Long suggestionId) {
        adminService.markSuggestionAsSeen(suggestionId);
        return ResponseEntity.ok(Map.of("message", "Suggestion marked as seen"));
    }
}
