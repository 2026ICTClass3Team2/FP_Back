package com.example.demo.domain.user.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@ToString
public class MemberDTO extends User {

    private String email;
    private String password;
    private String nickname;
    private List<String> roleNames;

    public MemberDTO(String email, String password, String nickname, List<String> roleNames) {
        super(email, password != null ? password : "", roleNames != null && !roleNames.isEmpty() ? 
                roleNames.stream().map(str -> new SimpleGrantedAuthority("ROLE_" + str)).collect(Collectors.toList()) : 
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        this.email = email;
        this.password = password != null ? password : "";
        this.nickname = nickname;
        this.roleNames = roleNames != null && !roleNames.isEmpty() ? roleNames : List.of("USER");
    }
}
