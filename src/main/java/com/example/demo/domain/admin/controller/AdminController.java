package com.example.demo.domain.admin.controller;

import com.example.demo.domain.admin.dto.AdminDashboardStatsDto;
import com.example.demo.domain.admin.dto.AdminUserDto;
import com.example.demo.domain.admin.dto.AdminChannelDto;
import com.example.demo.domain.admin.dto.ReportAdminDto;
import com.example.demo.domain.admin.dto.SuspendRequestDto;
import com.example.demo.domain.admin.service.AdminService;
import com.example.demo.domain.content.entity.ContentTag;
import com.example.demo.domain.content.entity.Post;
import com.example.demo.domain.content.repository.PostRepository;
import com.example.demo.domain.suggestion.entity.Suggestion;
import com.example.demo.domain.user.dto.MemberDTO;
import com.example.demo.global.elasticsearch.document.PostSearchDoc;
import com.example.demo.global.elasticsearch.repository.PostSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // Enable if Roles are setup properly with 'ROLE_ADMIN'
public class AdminController {

    private final AdminService adminService;
    private final PostRepository postRepository;
    private final PostSearchRepository postSearchRepository;

    @PostMapping("/sync-elasticsearch")
    public ResponseEntity<String> syncData() {
        // 1. Fetch all existing data from MySQL
        List<Post> allPosts = postRepository.findAll();

        // 2. Convert them to Search Documents
        List<PostSearchDoc> postDocs = allPosts.stream()
                .map(post -> {
                    List<String> tags = post.getContentTags().stream()
                            .map(ContentTag::getTagName)
                            .collect(Collectors.toList());
                    return new PostSearchDoc(post, tags);
                })
                .collect(Collectors.toList());

        // 3. Save them all to Elasticsearch at once
        postSearchRepository.saveAll(postDocs);

        return ResponseEntity.ok("Sync Complete!");
    }

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
    public ResponseEntity<?> warnUser(@PathVariable Long userId, @AuthenticationPrincipal MemberDTO memberDTO) {
        adminService.warnUser(userId, memberDTO.getId());
        return ResponseEntity.ok(Map.of("message", "User warned successfully"));
    }

    @PostMapping("/users/{userId}/suspend")
    public ResponseEntity<?> suspendUser(@PathVariable Long userId, @RequestBody SuspendRequestDto requestDto, @AuthenticationPrincipal MemberDTO memberDTO) {
        adminService.suspendUser(userId, memberDTO.getId(), requestDto);
        return ResponseEntity.ok(Map.of("message", "User suspended successfully"));
    }

    @PostMapping("/users/{userId}/revert-warn")
    public ResponseEntity<?> revertWarnUser(@PathVariable Long userId, @RequestBody(required = false) Map<String, String> body, @AuthenticationPrincipal MemberDTO memberDTO) {
        String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "I made a mistake";
        adminService.revertWarnUser(userId, memberDTO.getId(), reason);
        return ResponseEntity.ok(Map.of("message", "Warning reverted successfully"));
    }

    @PostMapping("/users/{userId}/revert-suspend")
    public ResponseEntity<?> revertSuspendUser(@PathVariable Long userId, @RequestBody(required = false) Map<String, String> body, @AuthenticationPrincipal MemberDTO memberDTO) {
        String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "I made a mistake";
        adminService.revertSuspendUser(userId, memberDTO.getId(), reason);
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
