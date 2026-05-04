package com.example.demo.domain.draft.service;

import com.example.demo.domain.draft.dto.DraftResponseDto;
import com.example.demo.domain.draft.dto.DraftSaveRequestDto;

import java.util.List;

public interface DraftService {
    Long createDraft(DraftSaveRequestDto dto, String email);
    void updateDraft(Long draftId, DraftSaveRequestDto dto, String email);
    List<DraftResponseDto> listDrafts(String email, String targetType);
    DraftResponseDto getDraft(Long draftId, String email);
    void deleteDraft(Long draftId, String email);
}
