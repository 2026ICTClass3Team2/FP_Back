package com.example.demo.domain.shop.service;

import com.example.demo.domain.shop.dto.EmoteResponseDto;
import com.example.demo.domain.shop.dto.EmoteUploadRequestDto;
import com.example.demo.domain.shop.dto.PurchaseHistoryDto;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface ShopService {

    Page<EmoteResponseDto> getEmotes(String sort, boolean purchasedOnly, boolean unpurchasedOnly,
                                     int page, int size, String email);

    EmoteResponseDto uploadEmote(EmoteUploadRequestDto dto);

    EmoteResponseDto updateEmote(Long id, EmoteUploadRequestDto dto);

    Map<String, Object> purchaseEmote(Long emoteId, String email);

    Page<PurchaseHistoryDto> getPurchaseHistory(String email, int page, int size);

    int getUserPoints(String email);
}
