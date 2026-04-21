package com.example.demo.domain.shop.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EmoteUploadRequestDto {

    private String name;
    private int price;
    private String imageUrl;
}
