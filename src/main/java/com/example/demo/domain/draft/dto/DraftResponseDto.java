package com.example.demo.domain.draft.dto;

import com.example.demo.domain.draft.entity.Draft;
import lombok.Builder;
import lombok.Data;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class DraftResponseDto {
    private Long draftId;
    private String title;
    private String body;
    private String targetType;
    private Long channelId;
    private List<String> tags;
    private String createdAt;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static DraftResponseDto from(Draft draft) {
        List<String> tags = draft.getContentTags().stream()
                .map(ct -> ct.getTag().getName())
                .collect(Collectors.toList());

        return DraftResponseDto.builder()
                .draftId(draft.getId())
                .title(draft.getTitle())
                .body(draft.getBody())
                .targetType(draft.getTargetType())
                .channelId(draft.getChannelId())
                .tags(tags)
                .createdAt(draft.getCreatedAt() != null
                        ? draft.getCreatedAt().format(FORMATTER) : null)
                .build();
    }
}
