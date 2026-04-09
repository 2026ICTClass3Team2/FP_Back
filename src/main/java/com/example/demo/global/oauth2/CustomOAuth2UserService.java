package com.example.demo.global.oauth2;

import com.example.demo.domain.user.dto.MemberDTO;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        log.info("getAttributes : {}", oAuth2User.getAttributes());

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, attributes);

        String provider = oAuth2UserInfo.getProvider();
        String providerId = oAuth2UserInfo.getProviderId();
        String email = oAuth2UserInfo.getEmail();
        String name = oAuth2UserInfo.getName();

        // 1. GitHub의 경우 이메일이 Private 상태면 null로 오기 때문에 추가 API 호출로 가져옵니다.
        if ("github".equals(registrationId) && (email == null || email.isEmpty() || email.equals("null"))) {
            email = getGithubEmail(userRequest.getAccessToken().getTokenValue());
        }

        // 2. 그럼에도 이메일을 가져오지 못했다면 임시 이메일 부여 (주로 카카오 선택 동의 안 한 경우)
        if (email == null || email.isEmpty() || email.equals("null")) {
             email = providerId + "@" + provider + ".com";
             log.warn("OAuth2 Email is null, using generated email: {}", email);
        }

        if (name == null || name.isEmpty() || name.equals("null")) {
             name = provider + "_" + providerId;
             log.warn("OAuth2 Name is null, using generated name: {}", name);
        }
        
        Optional<User> optionalUser = userRepository.findByProviderAndProviderId(provider, providerId);
        if(optionalUser.isEmpty()) {
             optionalUser = userRepository.findByEmail(email);
        }

        User user = null;

        if (optionalUser.isEmpty()) {
            log.info("OAuth2 Login: First time login. Proceed to join.");
            String randomPw = UUID.randomUUID().toString();
            
            user = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(randomPw))
                    .nickname(name)
                    .provider(provider)
                    .providerId(providerId)
                    .build();
                    
            user.addRole("USER");
            userRepository.save(user);
        } else {
            log.info("OAuth2 Login: Existing user.");
            user = optionalUser.get();
        }

        return new MemberDTO(
                user.getEmail(),
                user.getPassword(),
                user.getNickname(),
                user.getRoleList(),
                oAuth2User.getAttributes()
        );
    }

    // GitHub Private 이메일 조회 유틸 메서드
    private String getGithubEmail(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>("", headers);

        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    "https://api.github.com/user/emails",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<Map<String, Object>> emails = response.getBody();
            if (emails != null) {
                for (Map<String, Object> emailData : emails) {
                    // 주 사용(primary) 이메일이고 인증된(verified) 이메일을 찾아서 반환
                    if (Boolean.TRUE.equals(emailData.get("primary")) && Boolean.TRUE.equals(emailData.get("verified"))) {
                        return (String) emailData.get("email");
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch GitHub private email", e);
        }
        return null;
    }
}
