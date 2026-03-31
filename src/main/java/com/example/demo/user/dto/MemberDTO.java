package com.example.demo.user.dto;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MemberDTO extends User {
    private String email;
    private String pw;
    private String nickname;
    private boolean social;
    private List<String> roleName=new ArrayList<String>();
    public MemberDTO(String email,
                     String pw,
                     String nickname,boolean social,
                     List<String> roleName){
        super(email,
                pw,
                roleName.stream().map(s->new SimpleGrantedAuthority(s)).toList());
        this.email=email;
        this.pw=pw;
        this.nickname=nickname;
        this.social=social;
        this.roleName=roleName;
    }
    public Map<String,Object> getClaims(){ //JWT 관련123
        Map<String,Object> dataMap=new HashMap<>();
        dataMap.put("email", email);
        dataMap.put("pw", pw);
        dataMap.put("nickname", nickname);
        dataMap.put("social", social);
        dataMap.put("roleName", roleName);
        return dataMap;
    }
}