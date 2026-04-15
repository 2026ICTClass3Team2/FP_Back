package com.example.demo.domain.user.service;

import com.example.demo.domain.content.entity.Tag;
import com.example.demo.domain.content.repository.TagRepository;
import com.example.demo.domain.user.dto.UserJoinDTO;
import com.example.demo.domain.user.entity.*;
import com.example.demo.domain.user.repository.InterestRepository;
import com.example.demo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TagRepository tagRepository;
    private final InterestRepository interestRepository;

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
}
