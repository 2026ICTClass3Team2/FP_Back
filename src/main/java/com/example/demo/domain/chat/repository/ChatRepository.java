package com.example.demo.domain.chat.repository;

import com.example.demo.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * ChatRepository — 채팅 메시지 영속화를 위한 JPA 레포지토리입니다.
 *
 * 주요 기능:
 *  - 특정 대화방의 메시지 이력 조회 (최신순 페이징)
 *  - 사용자가 속한 모든 대화방의 마지막 메시지 및 대화 상대 정보 조회
 *  - 메시지 읽음 처리
 */
public interface ChatRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 특정 대화 ID에 속한 메시지들을 생성일시 내림차순(최신순)으로 페이징 조회합니다.
     */
    List<ChatMessage> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    /**
     * 특정 대화방에서 수신자가 '나'인 모든 읽지 않은 메시지를 읽음(is_read = true) 처리합니다.
     */
    @Modifying
    @Query("UPDATE ChatMessage c SET c.isRead = true WHERE c.conversationId = :conversationId AND c.receiver.id = :userId AND c.isRead = false")
    void markAsRead(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    /**
     * 사용자가 참여한 대화방 목록을 최신 메시지 순으로 가져옵니다.
     * nativeQuery를 사용하여 복잡한 그룹화 및 최신 레코드 추출을 수행합니다.
     * (여기서는 간단한 예시 쿼리이며, 실제 상용 레벨에서는 별도의 Conversation 테이블을 두는 것이 성능상 유리합니다.)
     */
    @Query(value = "SELECT * FROM chat WHERE chat_id IN (" +
                   "  SELECT MAX(chat_id) FROM chat " +
                   "  WHERE sender_id = :userId OR receiver_id = :userId " +
                   "  GROUP BY conversation_id" +
                   ") ORDER BY created_at DESC", nativeQuery = true)
    List<ChatMessage> findRecentConversations(@Param("userId") Long userId);

    /**
     * 특정 대화방에서 수신자가 '나'인 읽지 않은 메시지 수를 카운트합니다.
     */
    long countByConversationIdAndReceiverIdAndIsReadFalse(Long conversationId, Long userId);
}
