package com.example.demo.global.oauth2;

import com.example.demo.domain.notification.entity.NotificationSetting;
import com.example.demo.domain.notification.repository.NotificationSettingRepository;
import com.example.demo.domain.user.dto.MemberDTO;
import com.example.demo.domain.user.entity.Provider;
import com.example.demo.domain.user.entity.Role;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.entity.UserStatus;
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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationSettingRepository notificationSettingRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        log.info("getAttributes : {}", oAuth2User.getAttributes());

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, attributes);

        String providerStr = oAuth2UserInfo.getProvider();
        Provider provider;
        try {
            provider = Provider.valueOf(providerStr);
        } catch (IllegalArgumentException e) {
            log.error("Unsupported provider: {}", providerStr);
            provider = Provider.local; // fallback
        }
        
        String providerId = oAuth2UserInfo.getProviderId();
        String email = oAuth2UserInfo.getEmail();
        String name = oAuth2UserInfo.getName();

        // 1. GitHub의 경우 이메일이 Private 상태면 null로 오기 때문에 추가 API 호출로 가져옵니다.
        if ("github".equals(registrationId) && (email == null || email.isEmpty() || email.equals("null"))) {
            email = getGithubEmail(userRequest.getAccessToken().getTokenValue());
        }

        // 2. 그럼에도 이메일을 가져오지 못했다면 임시 이메일 부여 (주로 카카오 선택 동의 안 한 경우)
        if (email == null || email.isEmpty() || email.equals("null")) {
             email = providerId + "@" + providerStr + ".com";
             log.warn("OAuth2 Email is null, using generated email: {}", email);
        }

        if (name == null || name.isEmpty() || name.equals("null")) {
             name = providerStr + "_" + providerId;
             log.warn("OAuth2 Name is null, using generated name: {}", name);
        }
        
        Optional<User> optionalUser = userRepository.findByProviderAndProviderId(provider, providerId);
        if(optionalUser.isEmpty()) {
             optionalUser = userRepository.findByEmail(email);
        }

        User user = null;
        boolean isNewUser = false;

        if (optionalUser.isEmpty()) {
            log.info("OAuth2 Login: First time login. Proceed to join.");
            String randomPw = UUID.randomUUID().toString();
            // 임시 username — 이후 /member/oauth/setup-username 에서 사용자가 직접 설정
            String username = providerStr + "_" + providerId;
            while (userRepository.existsByUsername(username)) {
                username = username + "_" + UUID.randomUUID().toString().substring(0, 4);
            }
            user = User.builder()
                    .email(email)
                    .username(username)
                    .password(passwordEncoder.encode(randomPw))
                    .nickname(name)
                    .provider(provider)
                    .providerId(providerId)
                    .status(UserStatus.active)
                    .role(Role.user)
                    .build();
            userRepository.save(user);
            
            // Notification Setting Logic
            notificationSettingRepository.save(NotificationSetting.builder()
                    .user(user)
                    .build());
            
            isNewUser = true;
        } else {
            log.info("OAuth2 Login: Existing user.");
            user = optionalUser.get();
        }

        List<String> roleNames = List.of(user.getRole().name());

        return new MemberDTO(
                user.getEmail(),
                user.getPassword(),
                user.getNickname(),
                roleNames,
                oAuth2User.getAttributes(),
                isNewUser
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
