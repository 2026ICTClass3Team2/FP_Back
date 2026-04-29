package com.example.demo.domain.chat.controller;

import com.example.demo.domain.chat.dto.ChatMessageDto;
import com.example.demo.domain.chat.dto.ConversationSummaryDto;
import com.example.demo.domain.chat.service.ChatService;
import com.example.demo.domain.user.dto.MemberDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ChatController — 채팅 관련 REST API 엔드포인트를 제공합니다.
 *
 * 제공 기능:
 *  - 특정 사용자와의 채팅 이력 조회
 *  - 전체 대화 목록 조회
 *  - 읽음 처리
 */
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 특정 상대방과의 채팅 메시지 이력을 가져옵니다.
     * @param with 상대방 userId
     * @param page 페이지 번호 (0부터 시작)
     */
    @GetMapping("/history")
    public ResponseEntity<List<ChatMessageDto>> getHistory(
            @AuthenticationPrincipal MemberDTO memberDTO,
            @RequestParam("with") Long with,
            @RequestParam(value = "page", defaultValue = "0") int page
    ) {
        return ResponseEntity.ok(chatService.getHistory(memberDTO.getId(), with, page));
    }

    /**
     * 현재 사용자의 전체 대화 목록(파트너 정보 및 마지막 메시지)을 가져옵니다.
     */
    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationSummaryDto>> getConversations(
            @AuthenticationPrincipal MemberDTO memberDTO
    ) {
        return ResponseEntity.ok(chatService.getConversations(memberDTO.getId()));
    }

    /**
     * 특정 대화방의 메시지들을 모두 읽음 처리합니다.
     * @param partnerId 대화 상대방 ID
     */
    @PostMapping("/mark-read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal MemberDTO memberDTO,
            @RequestParam("partnerId") Long partnerId
    ) {
        chatService.markAsRead(memberDTO.getId(), partnerId);
        return ResponseEntity.ok().build();
    }
}
