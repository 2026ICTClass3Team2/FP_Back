package com.example.demo.domain.chat.entity;

import com.example.demo.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "chat", indexes = {
    @Index(name = "idx_chat_sender", columnList = "sender_id, created_at"),
    @Index(name = "idx_chat_receiver", columnList = "receiver_id, created_at"),
    @Index(name = "idx_chat_conversation", columnList = "conversation_id, created_at"),
    @Index(name = "idx_chat_read", columnList = "is_read")
})
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_id")
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private User receiver;
}
