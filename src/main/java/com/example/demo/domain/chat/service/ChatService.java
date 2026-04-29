package com.example.demo.domain.chat.service;

import com.example.demo.domain.chat.dto.ChatMessageDto;
import com.example.demo.domain.chat.dto.ConversationSummaryDto;
import com.example.demo.domain.chat.entity.ChatHidden;
import com.example.demo.domain.chat.entity.ChatMessage;
import com.example.demo.domain.chat.repository.ChatHiddenRepository;
import com.example.demo.domain.chat.repository.ChatRepository;
import com.example.demo.domain.notification.entity.NotificationTargetType;
import com.example.demo.domain.notification.service.NotificationService;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * ChatService — 1:1 채팅의 핵심 비즈니스 로직을 처리하는 서비스입니다.
 *
 * 역할:
 *  - 결정론적 대화 ID 생성 (1:1 매칭 보장)
 *  - 대화 이력 조회 및 DTO 변환
 *  - 새 메시지 저장 및 읽음 처리
 *  - 알림 서비스 연동
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatHiddenRepository chatHiddenRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * 두 사용자 간의 유일한 대화 ID를 생성합니다.
     * sender와 receiver의 ID 순서에 상관없이 동일한 결과를 반환합니다.
     * 공식: (min_id * 10,000,000) + max_id
     */
    public Long getConversationId(Long userA, Long userB) {
        long min = Math.min(userA, userB);
        long max = Math.max(userA, userB);
        return min * 10000000L + max;
    }

    /**
     * 특정 사용자와의 대화 이력을 가져옵니다.
     */
    @Transactional(readOnly = true)
    public List<ChatMessageDto> getHistory(Long myId, Long otherUserId, int page) {
        Long conversationId = getConversationId(myId, otherUserId);
        // 최신 메시지 20개씩 페이징
        List<ChatMessage> messages = chatRepository.findByConversationIdOrderByCreatedAtDesc(
                conversationId, PageRequest.of(page, 20));
        
        return messages.stream()
                .map(ChatMessageDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 새 메시지를 DB에 저장하고 관련 처리를 수행합니다.
     */
    @Transactional
    public ChatMessageDto saveMessage(Long senderId, Long receiverId, String content) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("Receiver not found"));

        Long convId = getConversationId(senderId, receiverId);

        ChatMessage chatMessage = ChatMessage.builder()
                .conversationId(convId)
                .sender(sender)
                .receiver(receiver)
                .content(content)
                .isRead(false)
                .build();

        ChatMessage saved = chatRepository.save(chatMessage);

        // 실시간 알림 전송 (채팅 타입)
        notificationService.sendNotification(
                receiver,
                "chat",
                NotificationTargetType.chat,
                senderId,
                sender.getNickname() + "님이 메시지를 보냈습니다."
        );

        log.info("[Chat] Message saved. convId={}, sender={}, receiver={}", convId, senderId, receiverId);
        return new ChatMessageDto(saved);
    }

    /**
     * 특정 대화방의 메시지를 읽음 처리합니다.
     */
    @Transactional
    public void markAsRead(Long myId, Long otherUserId) {
        Long conversationId = getConversationId(myId, otherUserId);
        chatRepository.markAsRead(conversationId, myId);
        
        User me = userRepository.findById(myId).orElse(null);
        if (me != null) {
            notificationService.markTargetAsRead(me, NotificationTargetType.chat, otherUserId);
        }
        
        log.debug("[Chat] Marked as read. convId={}, userId={}", conversationId, myId);
    }

    /**
     * 내가 참여한 대화 목록(최근 메시지 포함)을 가져옵니다.
     * 나가기 처리된 대화방은 새 메시지가 없으면 목록에서 제외됩니다.
     */
    @Transactional(readOnly = true)
    public List<ConversationSummaryDto> getConversations(Long myId) {
        List<ChatMessage> lastMessages = chatRepository.findRecentConversations(myId);

        // 내가 숨긴 대화방의 hiddenAt 시각을 파트너 ID 기준으로 매핑
        Map<Long, LocalDateTime> hiddenMap = chatHiddenRepository.findByUserId(myId).stream()
                .collect(Collectors.toMap(ChatHidden::getPartnerId, ChatHidden::getHiddenAt));

        return lastMessages.stream().map(msg -> {
            User partner = msg.getSender().getId().equals(myId) ? msg.getReceiver() : msg.getSender();

            // hiddenAt 이후 새 메시지가 없으면 목록에서 제외
            LocalDateTime hiddenAt = hiddenMap.get(partner.getId());
            if (hiddenAt != null && !msg.getCreatedAt().isAfter(hiddenAt)) return null;

            long unreadCount = chatRepository.countByConversationIdAndReceiverIdAndIsReadFalse(
                    msg.getConversationId(), myId);

            return new ConversationSummaryDto(
                    msg.getConversationId(),
                    partner.getId(),
                    partner.getNickname(),
                    partner.getProfilePicUrl(),
                    msg.getContent(),
                    msg.getCreatedAt(),
                    unreadCount
            );
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 특정 대화방을 목록에서 숨깁니다. 이후 새 메시지가 오면 자동으로 다시 표시됩니다.
     */
    @Transactional
    public void hideConversation(Long myId, Long partnerId) {
        chatHiddenRepository.deleteByUserIdAndPartnerId(myId, partnerId);
        chatHiddenRepository.save(ChatHidden.builder()
                .userId(myId)
                .partnerId(partnerId)
                .build());
        log.info("[Chat] Conversation hidden. userId={}, partnerId={}", myId, partnerId);
    }
}
