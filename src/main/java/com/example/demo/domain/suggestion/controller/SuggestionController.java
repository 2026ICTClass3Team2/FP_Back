package com.example.demo.domain.suggestion.controller;

import com.example.demo.domain.suggestion.dto.SuggestionRequestDto;
import com.example.demo.domain.suggestion.service.SuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/suggestions")
@RequiredArgsConstructor
public class SuggestionController {

    private final SuggestionService suggestionService;

    @PostMapping
    public ResponseEntity<?> createSuggestion(
            @RequestBody SuggestionRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        suggestionService.createSuggestion(requestDto, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Suggestion submitted successfully"));
    }
}
