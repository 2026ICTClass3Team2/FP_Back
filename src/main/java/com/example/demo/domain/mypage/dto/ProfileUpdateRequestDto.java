package com.example.demo.domain.mypage.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ProfileUpdateRequestDto {
    private String nickname;
    private String email;
    private String profilePicUrl;
    private List<String> techStacks;
}
