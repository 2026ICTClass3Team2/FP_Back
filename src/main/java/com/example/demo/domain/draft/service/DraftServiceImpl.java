package com.example.demo.domain.draft.service;

import com.example.demo.domain.content.entity.ContentTag;
import com.example.demo.domain.content.entity.Tag;
import com.example.demo.domain.content.repository.ContentTagRepository;
import com.example.demo.domain.content.repository.TagRepository;
import com.example.demo.domain.draft.dto.DraftResponseDto;
import com.example.demo.domain.draft.dto.DraftSaveRequestDto;
import com.example.demo.domain.draft.entity.Draft;
import com.example.demo.domain.draft.repository.DraftRepository;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DraftServiceImpl implements DraftService {

    private final DraftRepository draftRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final ContentTagRepository contentTagRepository;

    @Override
    @Transactional
    public Long createDraft(DraftSaveRequestDto dto, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Draft draft = Draft.builder()
                .title(dto.getTitle())
                .body(dto.getBody())
                .targetType(dto.getTargetType())
                .channelId(dto.getChannelId())
                .user(user)
                .build();

        Draft saved = draftRepository.save(draft);
        saveTags(saved, dto.getTags());
        return saved.getId();
    }

    @Override
    @Transactional
    public void updateDraft(Long draftId, DraftSaveRequestDto dto, String email) {
        Draft draft = draftRepository.findByIdAndUserEmail(draftId, email)
                .orElseThrow(() -> new IllegalArgumentException("Draft not found"));

        draft.setTitle(dto.getTitle());
        draft.setBody(dto.getBody());
        draft.setChannelId(dto.getChannelId());

        // 태그 갱신: 기존 삭제 후 재저장
        contentTagRepository.deleteAllByDraft(draft);
        draft.getContentTags().clear();
        saveTags(draft, dto.getTags());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DraftResponseDto> listDrafts(String email, String targetType) {
        List<Draft> drafts = draftRepository
                .findByUserEmailAndTargetTypeOrderByCreatedAtDesc(email, targetType);
        return drafts.stream()
                .map(DraftResponseDto::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DraftResponseDto getDraft(Long draftId, String email) {
        Draft draft = draftRepository.findByIdAndUserEmail(draftId, email)
                .orElseThrow(() -> new IllegalArgumentException("Draft not found"));
        return DraftResponseDto.from(draft);
    }

    @Override
    @Transactional
    public void deleteDraft(Long draftId, String email) {
        Draft draft = draftRepository.findByIdAndUserEmail(draftId, email)
                .orElseThrow(() -> new IllegalArgumentException("Draft not found"));
        draftRepository.delete(draft);
    }

    private void saveTags(Draft draft, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) return;
        for (String name : tagNames) {
            Tag tag = tagRepository.findByName(name)
                    .orElseGet(() -> tagRepository.save(Tag.builder().name(name).build()));
            ContentTag ct = ContentTag.builder().draft(draft).tag(tag).build();
            contentTagRepository.save(ct);
        }
    }
}
