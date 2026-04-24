package com.example.demo.domain.suggestion.service;

import com.example.demo.domain.suggestion.dto.SuggestionRequestDto;
import com.example.demo.domain.suggestion.entity.Suggestion;
import com.example.demo.domain.suggestion.repository.SuggestionRepository;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
                .orElseThrow(() -> new RuntimeException("User not found"));

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
