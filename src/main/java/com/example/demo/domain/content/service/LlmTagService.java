package com.example.demo.domain.content.service;

import com.example.demo.domain.algorithm.service.UserInterestService;
import com.example.demo.domain.content.entity.ContentTag;
import com.example.demo.domain.content.entity.Post;
import com.example.demo.domain.content.entity.Tag;
import com.example.demo.domain.content.repository.ContentTagRepository;
import com.example.demo.domain.content.repository.PostRepository;
import com.example.demo.domain.content.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmTagService {

    private final TagRepository tagRepository;
    private final ContentTagRepository contentTagRepository;
    private final PostRepository postRepository;
    private final WebClient webClient;
    private final UserInterestService userInterestService;

    @Value("${llm.service-url}")
    private String llmServiceUrl;

    @Async("llmTaskExecutor")
    public void assignTagsToPost(Long postId, String title, String body, List<String> existingTagNames, Long userId) {
        log.info("[LLM 태그] 시작. postId={}", postId);
        try {
            List<String> allTagNames = tagRepository.findAll()
                    .stream()
                    .map(Tag::getName)
                    .toList();

            log.info("[LLM 태그] DB 태그 목록 {}개. postId={}", allTagNames.size(), postId);

            if (allTagNames.isEmpty()) {
                log.warn("[LLM 태그] DB에 태그가 없어 종료. postId={}", postId);
                return;
            }

            Map<String, Object> requestBody = Map.of(
                    "title", title,
                    "body", body.replaceAll("<[^>]*>", ""),
                    "available_tags", allTagNames
            );

            log.info("[LLM 태그] FastAPI 호출. url={}/api/tags/suggest", llmServiceUrl);

            Map response = webClient.post()
                    .uri(llmServiceUrl + "/api/tags/suggest")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();

            if (response == null) {
                log.warn("[LLM 태그] FastAPI 응답 null. postId={}", postId);
                return;
            }

            List<String> selectedTags = (List<String>) response.get("tags");
            log.info("[LLM 태그] LLM 반환 태그={}, postId={}", selectedTags, postId);

            // getReferenceById: 프록시 사용으로 트랜잭션 커밋 타이밍 문제 회피
            Post post = postRepository.getReferenceById(postId);

            List<String> tagsToSave = selectedTags.stream()
                    .filter(allTagNames::contains)
                    .filter(tag -> !existingTagNames.contains(tag))
                    .distinct()
                    .toList();

            log.info("[LLM 태그] 저장할 태그={}, postId={}", tagsToSave, postId);

            tagsToSave.forEach(tagName ->
                    tagRepository.findByName(tagName).ifPresent(tag ->
                            contentTagRepository.save(
                                    ContentTag.builder().post(post).tag(tag).build()
                            )
                    )
            );

            log.info("[LLM 태그] 완료. postId={}, 저장된 태그={}", postId, tagsToSave);

            // LLM 태그까지 모두 저장된 후 관심도 반영 (사용자 태그 + LLM 태그 전체 대상)
            if (userId != null) {
                userInterestService.onPostWrite(userId, postId);
            }

        } catch (WebClientRequestException e) {
            log.warn("[LLM 태그] FastAPI 연결 실패 (서버 미실행 또는 네트워크 오류). postId={}, 원인={}", postId, e.getMessage());
        } catch (Exception e) {
            log.error("[LLM 태그] 예외 발생. postId={}", postId, e);
        }
    }
}
