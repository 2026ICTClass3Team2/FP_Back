package com.example.demo.domain.channel.controller;

import com.example.demo.domain.channel.dto.ChannelCreateResponseDto;
import com.example.demo.domain.channel.dto.ChannelDetailDto;
import com.example.demo.domain.channel.dto.ChannelSummaryDto;
import com.example.demo.domain.channel.dto.SubscriberDto;
import com.example.demo.domain.channel.service.ChannelService;
import com.example.demo.domain.content.dto.PostFeedResponseDto;
import com.example.demo.domain.content.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/channels")
public class Channel {

    private final ChannelService channelService;
    private final PostService postService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createChannel(
            @RequestParam("channelName") String channelName,
            @RequestParam("description") String description,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "techStacks", required = false) List<String> techStacks,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        if (channelName == null || channelName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "채널 이름을 입력해 주세요."));
        }
        if (description == null || description.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "채널 설명을 입력해 주세요."));
        }

        try {
            Long channelId = channelService.createChannel(channelName, description, image, techStacks, userDetails.getUsername());
            return ResponseEntity.ok(ChannelCreateResponseDto.builder()
                    .channelId(channelId)
                    .message("채널이 성공적으로 생성되었습니다.")
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create channel", e);
            return ResponseEntity.internalServerError().body(Map.of("message", "채널 생성 중 오류가 발생했습니다."));
        }
    }

    @PutMapping(value = "/{channelId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateChannel(
            @PathVariable Long channelId,
            @RequestParam("channelName") String channelName,
            @RequestParam("description") String description,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "techStacks", required = false) List<String> techStacks,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        try {
            channelService.updateChannel(channelId, channelName, description, image, techStacks, userDetails.getUsername());
            return ResponseEntity.ok(Map.of("message", "채널이 성공적으로 수정되었습니다."));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update channel", e);
            return ResponseEntity.internalServerError().body(Map.of("message", "채널 수정 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/subscribed")
    public ResponseEntity<?> getSubscribedChannels(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        List<ChannelSummaryDto> channels = channelService.getSubscribedChannels(userDetails.getUsername());
        return ResponseEntity.ok(channels);
    }

    @GetMapping("/popular")
    public ResponseEntity<List<ChannelSummaryDto>> getPopularChannels() {
        return ResponseEntity.ok(channelService.getPopularChannels());
    }

    @GetMapping("/search")
    public ResponseEntity<List<ChannelSummaryDto>> searchChannels(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(channelService.searchChannels(keyword, size));
    }

    @GetMapping("/{channelId}")
    public ResponseEntity<?> getChannelDetail(
            @PathVariable Long channelId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String username = userDetails != null ? userDetails.getUsername() : null;
            ChannelDetailDto detail = channelService.getChannelDetail(channelId, username);
            return ResponseEntity.ok(detail);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{channelId}/subscribe")
    public ResponseEntity<?> subscribe(
            @PathVariable Long channelId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        try {
            channelService.subscribeChannel(channelId, userDetails.getUsername());
            return ResponseEntity.ok(Map.of("message", "채널을 구독했습니다."));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{channelId}/subscribe")
    public ResponseEntity<?> unsubscribe(
            @PathVariable Long channelId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        try {
            channelService.unsubscribeChannel(channelId, userDetails.getUsername());
            return ResponseEntity.ok(Map.of("message", "채널 구독을 취소했습니다."));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{channelId}/subscribers")
    public ResponseEntity<List<SubscriberDto>> getSubscribers(@PathVariable Long channelId) {
        return ResponseEntity.ok(channelService.getSubscribers(channelId));
    }

    @GetMapping("/{channelId}/posts")
    public ResponseEntity<Slice<PostFeedResponseDto>> getChannelPosts(
            @PathVariable Long channelId,
            @RequestParam(required = false) Long lastPostId,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : null;
        Slice<PostFeedResponseDto> posts = postService.getChannelPosts(channelId, lastPostId, size, username);
        return ResponseEntity.ok(posts);
    }
}
