package com.example.demo.domain.notification.controller;

import com.example.demo.domain.notification.dto.NotificationResponseDto;
import com.example.demo.domain.notification.dto.NotificationSettingDto;
import com.example.demo.domain.notification.service.NotificationService;
import com.example.demo.domain.user.dto.MemberDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/recent")
    public ResponseEntity<List<NotificationResponseDto>> getRecentUnread(@AuthenticationPrincipal MemberDTO memberDTO) {
        List<NotificationResponseDto> list = notificationService.getRecentUnread(memberDTO.getEmail())
                .stream().map(NotificationResponseDto::new).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponseDto>> getAll(@AuthenticationPrincipal MemberDTO memberDTO,
                                                              @RequestParam(defaultValue = "all") String filter) {
        List<NotificationResponseDto> list = notificationService.getNotifications(memberDTO.getEmail(), filter)
                .stream().map(NotificationResponseDto::new).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PostMapping("/read")
    public ResponseEntity<Void> markAsRead(@AuthenticationPrincipal MemberDTO memberDTO,
                                         @RequestBody Map<String, List<Long>> payload) {
        notificationService.markAsRead(payload.get("ids"), memberDTO.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal MemberDTO memberDTO) {
        notificationService.markAllAsRead(memberDTO.getEmail());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/settings")
    public ResponseEntity<NotificationSettingDto> getSettings(@AuthenticationPrincipal MemberDTO memberDTO) {
        return ResponseEntity.ok(notificationService.getSettings(memberDTO.getEmail()));
    }

    @PutMapping("/settings")
    public ResponseEntity<Void> updateSettings(@AuthenticationPrincipal MemberDTO memberDTO,
                                             @RequestBody NotificationSettingDto settings) {
        notificationService.updateSettings(memberDTO.getEmail(), settings);
        return ResponseEntity.ok().build();
    }
}
