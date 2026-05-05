package com.example.demo.domain.qna.service;

import com.example.demo.domain.content.entity.Post;
import com.example.demo.domain.content.repository.PostRepository;
import com.example.demo.domain.qna.entity.Qna;
import com.example.demo.domain.qna.repository.QnaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventQnaScheduler {

    private final WebClient webClient;
    private final PostRepository postRepository;
    private final QnaRepository qnaRepository;

    @Value("${llm.service-url}")
    private String llmServiceUrl;

    /**
     * 매주 월요일 오전 9시에 이벤트 QnA 3문제를 자동 생성합니다.
     * (easy 10P / medium 15P / hard 20P)
     */
    @Scheduled(cron = "0 0 9 * * WED")
    @Transactional
    public void generateWeeklyEventQuestions() {
        log.info("[이벤트 QnA] 주간 이벤트 문제 생성 시작");
        try {
            Map<?, ?> responseBody = webClient.post()
                    .uri(llmServiceUrl + "/api/event/generate")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(90))
                    .block();

            if (responseBody == null || !responseBody.containsKey("questions")) {
                log.warn("[이벤트 QnA] LLM 응답이 비어 있습니다.");
                return;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> questions = (List<Map<String, Object>>) responseBody.get("questions");

            for (Map<String, Object> q : questions) {
                String title  = String.valueOf(q.get("title"));
                String body   = String.valueOf(q.get("body"));
                int points    = ((Number) q.get("points")).intValue();

                Post post = Post.builder()
                        .title(title)
                        .body(body)
                        .contentType("qna")
                        .build();
                Post savedPost = postRepository.save(post);

                Qna qna = Qna.builder()
                        .post(savedPost)
                        .rewardPoints(points)
                        .llmScore(0)
                        .isEvent(true)
                        .eventPoints(points)
                        .build();
                qnaRepository.save(qna);

                log.info("[이벤트 QnA] 생성 완료: title='{}', points={}", title, points);
            }

            log.info("[이벤트 QnA] 총 {}개 문제 생성 완료", questions.size());

        } catch (WebClientRequestException e) {
            log.error("[이벤트 QnA] LLM 서비스 연결 실패. 스케줄 재시도 예정. 원인={}", e.getMessage());
        } catch (Exception e) {
            log.error("[이벤트 QnA] 이벤트 문제 생성 중 오류 발생", e);
        }
    }
}
