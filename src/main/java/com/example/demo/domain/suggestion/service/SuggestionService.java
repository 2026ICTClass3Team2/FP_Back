package com.example.demo.domain.suggestion.service;

import com.example.demo.domain.suggestion.dto.SuggestionRequestDto;

public interface SuggestionService {
    void createSuggestion(SuggestionRequestDto requestDto, String email);
}
