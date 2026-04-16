package com.example.demo.domain.mypage.dto;

import com.example.demo.domain.report.entity.Block;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BlockResponseDto {
    private Long blockId;
    private Long blockedUserId;
    private String blockedUserNickname;
    private String blockedUserProfileImageUrl;
    private LocalDateTime blockedAt;

    public static BlockResponseDto from(Block block) {
        return BlockResponseDto.builder()
                .blockId(block.getId())
                .blockedUserId(block.getBlocked().getId())
                .blockedUserNickname(block.getBlocked().getNickname())
                .blockedUserProfileImageUrl(block.getBlocked().getProfilePicUrl())
                .blockedAt(block.getCreatedAt())
                .build();
    }
}
