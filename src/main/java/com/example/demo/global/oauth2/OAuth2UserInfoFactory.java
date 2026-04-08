package com.example.demo.global.oauth2;

import java.util.Map;

public class OAuth2UserInfoFactory {
    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        if (registrationId.equalsIgnoreCase("google")) {
            return new GoogleUserInfo(attributes);
        } else if (registrationId.equalsIgnoreCase("naver")) {
            return new NaverUserInfo(attributes);
        } else if (registrationId.equalsIgnoreCase("kakao")) {
            return new KakaoUserInfo(attributes);
        } else if (registrationId.equalsIgnoreCase("github")) {
            return new GithubUserInfo(attributes);
        } else {
            throw new IllegalArgumentException("Unsupported Login Type : " + registrationId);
        }
    }
}
