package com.example.demo.global.oauth2;

import java.util.Map;

public class GithubUserInfo implements OAuth2UserInfo {

    private Map<String, Object> attributes;

    public GithubUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getProvider() {
        return "github";
    }

    @Override
    public String getEmail() {
        // github의 경우 email이 private 설정되어있을 수 있어 응답에 없을 수 있습니다.
        // 이때 로그인 id 등 다른 값을 이메일처럼 사용하도록 방어 코드가 필요할 수 있습니다.
        Object email = attributes.get("email");
        return email != null ? String.valueOf(email) : attributes.get("login") + "@github.com";
    }

    @Override
    public String getName() {
        Object name = attributes.get("name");
        return name != null ? String.valueOf(name) : String.valueOf(attributes.get("login"));
    }
}
