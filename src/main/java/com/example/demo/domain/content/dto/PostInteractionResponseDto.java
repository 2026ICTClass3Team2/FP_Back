package com.example.demo.domain.content.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostInteractionResponseDto {
    private int likeCount;
    private int dislikeCount;
    @JsonProperty("isLiked")
    private boolean isLiked;
    @JsonProperty("isDisliked")
    private boolean isDisliked;
}
