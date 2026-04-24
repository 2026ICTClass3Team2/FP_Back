package com.example.demo.domain.user.dto;

import com.example.demo.domain.user.entity.User;
import lombok.Data;

@Data
public class UserSummaryDTO {
    private Long id;
    private String nickname;
    private String username;
    private String profilePicUrl;

    public UserSummaryDTO(User user) {
        this.id = user.getId();
        this.nickname = user.getNickname();
        this.username = user.getUsername();
        this.profilePicUrl = user.getProfilePicUrl();
    }
}
