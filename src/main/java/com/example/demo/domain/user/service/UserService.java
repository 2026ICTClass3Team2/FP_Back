package com.example.demo.domain.user.service;

import com.example.demo.domain.user.dto.UserJoinDTO;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void join(UserJoinDTO userJoinDTO) {
        String email = userJoinDTO.getEmail();
        boolean exist = userRepository.existsByEmail(email);
        
        if (exist) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = User.builder()
                .email(userJoinDTO.getEmail())
                .password(passwordEncoder.encode(userJoinDTO.getPassword()))
                .nickname(userJoinDTO.getNickname())
                .build();
                
        user.addRole("USER");

        userRepository.save(user);
    }
}
