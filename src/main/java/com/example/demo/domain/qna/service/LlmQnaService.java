package com.example.demo.domain.qna.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmQnaService {

    private final WebClient webClient;

    @Value("${llm.service-url}")
    private String llmServiceUrl;

    /**
     * 질문 제목·본문을 LLM에 보내 답변 난이도(1~10)를 채점한다.
     * LLM 서비스가 불가할 경우 기본값 5를 반환한다.
     */
    public int scoreQnaDifficulty(String title, String body) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "title", title != null ? title : "",
                    "body", body != null ? body : ""
            );

            Map<?, ?> response = webClient.post()
                    .uri(llmServiceUrl + "/api/qna/score")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();

            if (response == null || !response.containsKey("score")) {
                log.warn("[LLM QnA 난이도] 응답 없음. 기본값 5 반환");
                return 5;
            }

            int score = ((Number) response.get("score")).intValue();
            score = Math.max(1, Math.min(10, score));
            log.info("[LLM QnA 난이도] 채점 완료. score={}", score);
            return score;

        } catch (WebClientRequestException e) {
            log.warn("[LLM QnA 난이도] FastAPI 연결 실패. 기본값 5 반환. 원인={}", e.getMessage());
            return 5;
        } catch (Exception e) {
            log.error("[LLM QnA 난이도] 예외 발생. 기본값 5 반환", e);
            return 5;
        }
    }
}
