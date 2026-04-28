package com.example.demo.domain.favorite.dto;

import com.example.demo.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FavoriteUserDto {
    private Long userId;
    private String nickname;
    private String username;
    private String profilePicUrl;

    public static FavoriteUserDto from(User user) {
        return FavoriteUserDto.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .username(user.getUsername())
                .profilePicUrl(user.getProfilePicUrl())
                .build();
    }
}
