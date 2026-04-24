package com.example.demo.domain.user.service;

import com.example.demo.domain.content.entity.Tag;
import com.example.demo.domain.content.repository.TagRepository;
import com.example.demo.domain.notification.entity.NotificationSetting;
import com.example.demo.domain.notification.repository.NotificationSettingRepository;
import com.example.demo.domain.user.dto.UserJoinDTO;
import com.example.demo.domain.user.dto.UserSummaryDTO;
import com.example.demo.domain.user.entity.*;
import com.example.demo.domain.user.repository.InterestRepository;
import com.example.demo.domain.user.repository.UserRepository;
import com.example.demo.global.redis.RedisService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TagRepository tagRepository;
    private final InterestRepository interestRepository;
    private final RedisService redisService;
    private final MailService mailService;
    private final NotificationSettingRepository notificationSettingRepository;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Transactional
    public void join(UserJoinDTO userJoinDTO) {
        String email = userJoinDTO.getEmail();
        String username = userJoinDTO.getUsername();
        
        // 이메일 중복 체크
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        
        // username 중복 체크
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        // 1. 유저 저장
        User user = User.builder()
                .email(email)
                .username(username)
                .password(passwordEncoder.encode(userJoinDTO.getPassword()))
                .nickname(userJoinDTO.getNickname())
                .provider(Provider.local) // 일반 가입
                .status(UserStatus.active)
                .role(Role.user)
                .build();
                
        User savedUser = userRepository.save(user);

        // 1.5. 알림 설정 초기화
        notificationSettingRepository.save(NotificationSetting.builder()
                .user(savedUser)
                .build());

        // 2. 관심사 저장 (선택 입력)
        List<String> techStacks = userJoinDTO.getTechStacks();
        if (techStacks != null && !techStacks.isEmpty()) {
            for (String stackName : techStacks) {
                Tag tag = tagRepository.findByName(stackName) // Changed from findByTagName
                        .orElseGet(() -> tagRepository.save(Tag.builder().name(stackName).build())); // Changed from tagName

                Interest interest = Interest.builder()
                        .user(savedUser)
                        .tag(tag)
                        .isProfileTag(true)
                        .build();
                interestRepository.save(interest);
            }
        }
    }

    // 아이디 중복 체크 API용
    public boolean checkUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    // 이메일 중복 체크 API용
    public boolean checkEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // OAuth 신규 가입 후 username 설정
    @Transactional
    public void setupOAuthUsername(String email, String username) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        if (user.getProvider() == Provider.local) {
            throw new IllegalArgumentException("소셜 로그인 사용자만 이용 가능합니다.");
        }
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        if (!username.matches("^[a-z0-9]{4,20}$")) {
            throw new IllegalArgumentException("아이디는 영문 소문자와 숫자 4~20자리여야 합니다.");
        }

        user.setUsername(username);
        userRepository.save(user);
    }

    // 비밀번호 찾기 — 재설정 메일 발송 (local 유저만)
    @Transactional
    public void sendPasswordResetEmail(String email) throws MessagingException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        if (user.getProvider() != Provider.local) {
            throw new IllegalArgumentException("소셜 로그인 계정은 비밀번호 찾기를 이용할 수 없습니다.");
        }

        String token = UUID.randomUUID().toString();
        redisService.savePasswordResetToken(token, email);

        String resetLink = frontendUrl + "/reset-password?token=" + token;
        mailService.sendPasswordResetEmail(email, resetLink);
    }

    // 비밀번호 재설정 토큰 유효성 검사
    public boolean verifyPasswordResetToken(String token) {
        return redisService.getPasswordResetEmail(token) != null;
    }

    // 비밀번호 재설정
    @Transactional
    public void resetPassword(String token, String newPassword) {
        String email = redisService.getPasswordResetEmail(token);
        if (email == null) {
            throw new IllegalArgumentException("비밀번호 재설정 링크가 만료되었거나 유효하지 않습니다.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        if (user.getProvider() != Provider.local) {
            throw new IllegalArgumentException("소셜 로그인 계정은 비밀번호를 변경할 수 없습니다.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        redisService.deletePasswordResetToken(token); // 일회성 토큰 삭제
    }

    public List<UserSummaryDTO> searchUsersForMention(String query) {
        return userRepository.findTop5ByNicknameContainingOrUsernameContaining(query, query)
                .stream()
                .map(UserSummaryDTO::new)
                .collect(java.util.stream.Collectors.toList());
    }
}
