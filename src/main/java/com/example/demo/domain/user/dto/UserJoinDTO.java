package com.example.demo.domain.user.dto;

import lombok.Data;

@Data
public class UserJoinDTO {
    private String email;
    private String password;
    private String nickname;
}
