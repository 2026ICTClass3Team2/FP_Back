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
import java.util.Optional;
import java.util.stream.Collectors;

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
        Optional<User> result = userRepository.getWithRoles(username);

        if(result.isEmpty()) {
            throw new UsernameNotFoundException("Not Found");
        }

        User user = result.get();
        
        List<String> roleNames = user.getRoleList().stream()
                                     .map(Enum::name)
                                     .collect(Collectors.toList());

        MemberDTO memberDTO = new MemberDTO(
                user.getEmail(),
                user.getPassword(),
                user.getNickname(),
                roleNames
        );

        return memberDTO;
    }
}
