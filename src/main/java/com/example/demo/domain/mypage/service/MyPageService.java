package com.example.demo.domain.mypage.service;

import com.example.demo.domain.content.entity.Tag;
import com.example.demo.domain.content.repository.TagRepository;
import com.example.demo.domain.mypage.dto.MyPageProfileResponseDto;
import com.example.demo.domain.mypage.dto.PasswordUpdateRequestDto;
import com.example.demo.domain.mypage.dto.ProfileUpdateRequestDto;
import com.example.demo.domain.user.entity.Interest;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.InterestRepository;
import com.example.demo.domain.user.repository.UserRepository;
import com.example.demo.domain.user.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final UserRepository userRepository;
    private final InterestRepository interestRepository;
    private final TagRepository tagRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    @Transactional(readOnly = true)
    public MyPageProfileResponseDto getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<String> techStacks = interestRepository.findByUserIdAndIsProfileTagTrue(user.getId())
                .stream()
                .map(interest -> interest.getTag().getName())
                .collect(Collectors.toList());

        return MyPageProfileResponseDto.builder()
                .profilePicUrl(user.getProfilePicUrl())
                .nickname(user.getNickname())
                .username(user.getUsername())
                .email(user.getEmail())
                .registeredAt(user.getRegisteredAt())
                .currentPoint(user.getCurrentPoint())
                .techStacks(techStacks)
                .build();
    }

    @Transactional
    public void updateProfile(String email, ProfileUpdateRequestDto requestDto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 닉네임 변경 (중복 검사 필요)
        if (requestDto.getNickname() != null && !requestDto.getNickname().equals(user.getNickname())) {
            if (userRepository.existsByNickname(requestDto.getNickname())) {
                throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
            }
            user.setNickname(requestDto.getNickname());
        }

        // 프로필 이미지 변경
        if (requestDto.getProfilePicUrl() != null) {
            user.setProfilePicUrl(requestDto.getProfilePicUrl());
        }

        // 기술 스택 변경
        if (requestDto.getTechStacks() != null) {
            // 기존 프로필 기술 스택 삭제
            interestRepository.deleteByUserIdAndIsProfileTagTrue(user.getId());

            // 새로운 기술 스택 저장
            for (String tagName : requestDto.getTechStacks()) {
                Tag tag = tagRepository.findByName(tagName)
                        .orElseGet(() -> tagRepository.save(Tag.builder().name(tagName).build()));
                
                Interest interest = Interest.builder()
                        .user(user)
                        .tag(tag)
                        .isProfileTag(true)
                        .weightScore(5.0) // 프로필 지정 스택은 가중치를 높게 줌
                        .build();
                interestRepository.save(interest);
            }
        }
    }

    @Transactional
    public void updatePassword(String email, PasswordUpdateRequestDto requestDto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(requestDto.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        user.setPassword(passwordEncoder.encode(requestDto.getNewPassword()));
    }

    public void requestEmailVerification(String newEmail) {
        if (userRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        try {
            // 이메일 변경용 메일 발송 메서드 호출
            mailService.sendUpdateVerificationMessage(newEmail);
        } catch (MessagingException e) {
            throw new RuntimeException("이메일 발송에 실패했습니다.", e);
        }
    }

    @Transactional
    public void verifyEmailAndChange(String currentEmail, String newEmail, String code) {
        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (userRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        boolean isVerified = mailService.verifyAuthCode(newEmail, code);
        if (!isVerified) {
            throw new IllegalArgumentException("인증번호가 일치하지 않거나 만료되었습니다.");
        }

        user.setEmail(newEmail);
    }
}
