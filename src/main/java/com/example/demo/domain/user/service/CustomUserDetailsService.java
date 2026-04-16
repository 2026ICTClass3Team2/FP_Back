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

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("loadUserByUsername: {}", username);

        // SecurityConfig에서 usernameParameter("email") 로 설정했기 때문에 
        // 여기서 매개변수 username은 실제로는 클라이언트가 입력한 email 값입니다.
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Not Found"));

        List<String> roleNames = List.of(user.getRole().name());

        return new MemberDTO(
                user.getEmail(),
                user.getPassword(),
                user.getNickname(),
                roleNames
        );
    }
}
