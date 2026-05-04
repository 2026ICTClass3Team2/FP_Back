package com.example.demo.domain.chatbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final WebClient webClient;

    @Value("${llm.service-url}")
    private String llmServiceUrl;

    public Map<?, ?> reviewCode(String code) {
        try {
            Map<?, ?> response = webClient.post()
                    .uri(llmServiceUrl + "/api/chatbot/review")
                    .bodyValue(Map.of("code", code))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();
            return response != null ? response : Map.of("review", "응답이 없습니다.");
        } catch (WebClientRequestException e) {
            log.warn("[Chatbot] LangChain 연결 실패: {}", e.getMessage());
            return Map.of("review", "AI 서버에 연결할 수 없습니다.");
        } catch (Exception e) {
            log.error("[Chatbot] 코드 리뷰 오류", e);
            return Map.of("review", "오류가 발생했습니다.");
        }
    }

    public Map<?, ?> chat(String message, List<String> history) {
        try {
            Map<?, ?> response = webClient.post()
                    .uri(llmServiceUrl + "/api/chatbot/chat")
                    .bodyValue(Map.of("message", message, "history", history != null ? history : List.of()))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();
            return response != null ? response : Map.of("response", "응답이 없습니다.");
        } catch (WebClientRequestException e) {
            log.warn("[Chatbot] LangChain 연결 실패: {}", e.getMessage());
            return Map.of("response", "AI 서버에 연결할 수 없습니다.");
        } catch (Exception e) {
            log.error("[Chatbot] 채팅 오류", e);
            return Map.of("response", "오류가 발생했습니다.");
        }
    }
}