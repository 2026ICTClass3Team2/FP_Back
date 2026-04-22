package com.example.demo.domain.shop.service;

import com.example.demo.domain.shop.dto.EmoteResponseDto;
import com.example.demo.domain.shop.dto.EmoteUploadRequestDto;
import com.example.demo.domain.shop.dto.PurchaseHistoryDto;
import com.example.demo.domain.shop.entity.Emote;
import com.example.demo.domain.shop.entity.Inventory;
import com.example.demo.domain.shop.repository.InventoryRepository;
import com.example.demo.domain.shop.repository.ShopRepository;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private final ShopRepository shopRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<EmoteResponseDto> getEmotes(String sort, boolean purchasedOnly, boolean unpurchasedOnly,
                                            int page, int size, String email) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));

        if (email == null) {
            return shopRepository.findAll(pageable)
                    .map(e -> EmoteResponseDto.from(e, false));
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (purchasedOnly) {
            return shopRepository.findPurchasedByUser(user.getId(), pageable)
                    .map(e -> EmoteResponseDto.from(e, true));
        }

        if (unpurchasedOnly) {
            return shopRepository.findUnpurchasedByUser(user.getId(), pageable)
                    .map(e -> EmoteResponseDto.from(e, false));
        }

        List<Long> purchasedIds = shopRepository.findPurchasedEmoteIdsByUser(user.getId());
        return shopRepository.findAll(pageable)
                .map(e -> EmoteResponseDto.from(e, purchasedIds.contains(e.getId())));
    }

    @Override
    @Transactional
    public EmoteResponseDto uploadEmote(EmoteUploadRequestDto dto) {
        Emote emote = new Emote();
        emote.setName(dto.getName());
        emote.setPrice(dto.getPrice());
        emote.setImageUrl(dto.getImageUrl());
        Emote saved = shopRepository.save(emote);
        return EmoteResponseDto.from(saved, false);
    }

    @Override
    @Transactional
    public EmoteResponseDto updateEmote(Long id, EmoteUploadRequestDto dto) {
        Emote emote = shopRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("이모티콘을 찾을 수 없습니다."));

        emote.setName(dto.getName());
        emote.setPrice(dto.getPrice());
        if (dto.getImageUrl() != null && !dto.getImageUrl().isEmpty()) {
            emote.setImageUrl(dto.getImageUrl());
        }

        return EmoteResponseDto.from(shopRepository.save(emote), false);
    }

    @Override
    @Transactional
    public Map<String, Object> purchaseEmote(Long emoteId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Emote emote = shopRepository.findById(emoteId)
                .orElseThrow(() -> new IllegalArgumentException("이모티콘을 찾을 수 없습니다."));

        if (inventoryRepository.existsByUserAndEmote_Id(user, emoteId)) {
            throw new IllegalStateException("이미 구매한 이모티콘입니다.");
        }

        if (user.getCurrentPoint() < emote.getPrice()) {
            throw new IllegalStateException("포인트가 부족합니다.");
        }

        user.setCurrentPoint(user.getCurrentPoint() - emote.getPrice());
        userRepository.save(user);

        inventoryRepository.save(new Inventory(user, emote));

        return Map.of(
                "message", "구매가 완료되었습니다.",
                "remainingPoints", user.getCurrentPoint()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseHistoryDto> getPurchaseHistory(String email, int page, int size) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Pageable pageable = PageRequest.of(page, size);
        return inventoryRepository.findByUserOrderByPurchasedAtDesc(user, pageable)
                .map(PurchaseHistoryDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public int getUserPoints(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return user.getCurrentPoint();
    }

    private Sort buildSort(String sort) {
        return switch (sort != null ? sort : "latest") {
            case "oldest" -> Sort.by(Sort.Direction.ASC, "id");
            case "priceAsc" -> Sort.by(Sort.Direction.ASC, "price");
            case "priceDesc" -> Sort.by(Sort.Direction.DESC, "price");
            default -> Sort.by(Sort.Direction.DESC, "id");
        };
    }
}
