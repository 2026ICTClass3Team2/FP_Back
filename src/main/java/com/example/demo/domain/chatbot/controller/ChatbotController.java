package com.example.demo.domain.chatbot.controller;

import com.example.demo.domain.chatbot.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/review")
    public ResponseEntity<Map<?, ?>> reviewCode(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(chatbotService.reviewCode(body.get("code")));
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<?, ?>> chat(@RequestBody Map<String, Object> body) {
        String message = (String) body.get("message");
        @SuppressWarnings("unchecked")
        List<String> history = (List<String>) body.getOrDefault("history", List.of());
        return ResponseEntity.ok(chatbotService.chat(message, history));
    }
}