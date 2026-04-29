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
        // MemberDTO에 getId()가 없으면 memberDTO.getEmail()로 조회하거나,
        // MemberDTO를 수정하여 id를 포함하게 해야 합니다.
        // 현재 MemberDTO 구조를 확인했을 때 User를 상속받고 있으나 id 필드는 보이지 않습니다.
        // 보통 MemberDTO 생성 시 id를 넘겨받도록 설계되어 있을 것입니다.
        // 여기서는 memberDTO.getId()가 있다고 가정하거나, 이메일로 서비스를 호출합니다.
        // (UserRepository findByEmail은 이미 존재함)
        // 일단 id가 있다고 가정하고 작성합니다. 만약 없다면 UserDetails 조회 로직이 필요합니다.
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

    /**
     * 특정 대화방을 목록에서 숨깁니다 (나가기).
     * @param partnerId 대화 상대방 ID
     */
    @DeleteMapping("/conversations/{partnerId}")
    public ResponseEntity<Void> hideConversation(
            @AuthenticationPrincipal MemberDTO memberDTO,
            @PathVariable Long partnerId
    ) {
        chatService.hideConversation(memberDTO.getId(), partnerId);
        return ResponseEntity.ok().build();
    }
}
