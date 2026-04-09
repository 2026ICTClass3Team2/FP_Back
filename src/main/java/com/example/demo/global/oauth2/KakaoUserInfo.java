package com.example.demo.global.oauth2;

import java.util.Map;

public class KakaoUserInfo implements OAuth2UserInfo {

    private Map<String, Object> attributes;
    private Map<String, Object> attributesAccount;
    private Map<String, Object> attributesProfile;

    public KakaoUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
        this.attributesAccount = (Map<String, Object>) attributes.get("kakao_account");
        
        if (this.attributesAccount != null) {
            this.attributesProfile = (Map<String, Object>) attributesAccount.get("profile");
        }
    }

    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getProvider() {
        return "kakao";
    }

    @Override
    public String getEmail() {
        if (attributesAccount != null && attributesAccount.get("email") != null) {
            return String.valueOf(attributesAccount.get("email"));
        }
        return null;
    }

    @Override
    public String getName() {
        // 실명(name)을 먼저 확인하고, 없으면 닉네임(nickname) 반환
        if (attributesAccount != null && attributesAccount.get("name") != null) {
            return String.valueOf(attributesAccount.get("name"));
        }
        if (attributesProfile != null && attributesProfile.get("nickname") != null) {
            return String.valueOf(attributesProfile.get("nickname"));
        }
        return null;
    }
}
