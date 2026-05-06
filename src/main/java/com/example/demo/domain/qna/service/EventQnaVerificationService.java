package com.example.demo.domain.qna.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventQnaVerificationService {

    private final WebClient webClient;
    private final EventQnaRewardService eventQnaRewardService;

    @Value("${llm.service-url}")
    private String llmServiceUrl;

    @Async("llmTaskExecutor")
    public void verifyAndRewardAsync(Long postId, Long commentId, Long commenterId,
                                     String questionTitle, String questionBody, String commentBody) {
        try {
            Map<String, String> requestBody = Map.of(
                    "question_title", questionTitle,
                    "question_body", questionBody,
                    "comment_body", commentBody
            );

            Map<?, ?> response = webClient.post()
                    .uri(llmServiceUrl + "/api/event/verify")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();

            if (response != null && Boolean.TRUE.equals(response.get("is_answer"))) {
                log.info("[이벤트 QnA] 검증 통과 — 포인트 지급 시작: postId={}, commentId={}", postId, commentId);
                eventQnaRewardService.awardIfNotYetSolved(postId, commentId, commenterId);
            } else {
                log.info("[이벤트 QnA] 검증 불통과: postId={}, commentId={}", postId, commentId);
            }
        } catch (Exception e) {
            log.warn("[이벤트 QnA] 답변 검증 실패 (포인트 미지급): postId={}, commentId={}, 오류={}",
                    postId, commentId, e.getMessage());
        }
    }
}
