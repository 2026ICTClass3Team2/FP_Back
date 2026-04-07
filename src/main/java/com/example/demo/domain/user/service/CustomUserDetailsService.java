package com.example.demo.domain.user.service;

import com.example.demo.domain.user.dto.MemberDTO;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("loadUserByUsername: {}", username);

        Optional<User> result = userRepository.getWithRoles(username);

        if(result.isEmpty()) {
            throw new UsernameNotFoundException("Not Found");
        }

        User user = result.get();

        MemberDTO memberDTO = new MemberDTO(
                user.getEmail(),
                user.getPassword(),
                user.getNickname(),
                user.getRoleList()
        );

        return memberDTO;
    }
}
