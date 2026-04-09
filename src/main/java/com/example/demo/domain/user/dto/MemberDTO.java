package com.example.demo.domain.user.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
@ToString
public class MemberDTO extends User implements OAuth2User { // OAuth2User 인터페이스 구현

    private String email;
    private String password;
    private String nickname;
    private List<String> roleNames;
    private Map<String, Object> attributes; // OAuth2User 속성 저장

    // 일반 로그인용 생성자
    public MemberDTO(String email, String password, String nickname, List<String> roleNames) {
        super(email, password != null ? password : "", roleNames != null && !roleNames.isEmpty() ?
                roleNames.stream().map(str -> new SimpleGrantedAuthority("ROLE_" + str.toUpperCase())).collect(Collectors.toList()) :
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        this.email = email;
        this.password = password != null ? password : "";
        this.nickname = nickname;
        this.roleNames = roleNames != null && !roleNames.isEmpty() ? roleNames : List.of("USER");
    }

    // OAuth2 로그인용 생성자
    public MemberDTO(String email, String password, String nickname, List<String> roleNames, Map<String, Object> attributes) {
        this(email, password, nickname, roleNames); // 기존 생성자 호출
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

    @Override
    public String getName() {
        // OAuth2User의 getName()은 주로 고유 식별자를 반환합니다.
        // 여기서는 email을 사용하겠습니다.
        return this.email;
    }
}
