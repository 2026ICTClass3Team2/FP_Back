package com.example.demo.domain.chat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_hidden", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "partner_id"})
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHidden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_hidden_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "partner_id", nullable = false)
    private Long partnerId;

    // 이 시각 이전의 마지막 메시지를 가진 대화방은 목록에서 숨깁니다.
    // 이후에 새 메시지가 오면 자동으로 다시 노출됩니다.
    @Column(name = "hidden_at", nullable = false)
    @Builder.Default
    private LocalDateTime hiddenAt = LocalDateTime.now();
}
