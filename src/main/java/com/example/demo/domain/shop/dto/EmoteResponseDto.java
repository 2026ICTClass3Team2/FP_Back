package com.example.demo.domain.shop.dto;

import com.example.demo.domain.shop.entity.Emote;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EmoteResponseDto {

    private Long id;
    private String name;
    private int price;
    private String imageUrl;
    private boolean purchased;

    public static EmoteResponseDto from(Emote emote, boolean purchased) {
        return new EmoteResponseDto(
                emote.getId(),
                emote.getName(),
                emote.getPrice(),
                emote.getImageUrl(),
                purchased
        );
    }
}
