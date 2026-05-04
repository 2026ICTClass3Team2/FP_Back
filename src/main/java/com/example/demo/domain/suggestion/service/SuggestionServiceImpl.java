package com.example.demo.domain.suggestion.service;

import com.example.demo.domain.suggestion.dto.SuggestionRequestDto;
import com.example.demo.domain.suggestion.entity.Suggestion;
import com.example.demo.domain.suggestion.repository.SuggestionRepository;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SuggestionServiceImpl implements SuggestionService {

    private final SuggestionRepository suggestionRepository;
    private final UserRepository userRepository;

    @Override
    public void createSuggestion(SuggestionRequestDto requestDto, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("해당 이메일을 가진 사용자를 찾을 수 없습니다: " + email));

        Suggestion suggestion = Suggestion.builder()
                .category(requestDto.getCategory())
                .title(requestDto.getTitle())
                .details(requestDto.getDetails())
                .user(user)
                .isSeen(false)
                .build();

        suggestionRepository.save(suggestion);
    }
}
