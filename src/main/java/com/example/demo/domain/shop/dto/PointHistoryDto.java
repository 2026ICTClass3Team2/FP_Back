package com.example.demo.domain.shop.dto;

import com.example.demo.domain.point.entity.PointTransaction;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
public class PointHistoryDto {

    private final String description;
    private final int pointChange;
    private final LocalDateTime createdAt;

    private static final Map<String, String> TYPE_LABEL = Map.of(
            "emote",   "이모티콘 구매",
            "admin",   "관리자 지급",
            "bonus",   "보너스 포인트",
            "qna",     "QnA 참여",
            "post",    "게시글 작성",
            "comment", "댓글 작성",
            "like",    "좋아요"
    );

    private PointHistoryDto(PointTransaction tx) {
        this.description = TYPE_LABEL.getOrDefault(tx.getTargetType(), "포인트 변동");
        this.pointChange = tx.getPointChange();
        this.createdAt   = tx.getCreatedAt();
    }

    public static PointHistoryDto from(PointTransaction tx) {
        return new PointHistoryDto(tx);
    }
}
