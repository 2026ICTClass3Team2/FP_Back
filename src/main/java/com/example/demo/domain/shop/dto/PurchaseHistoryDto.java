package com.example.demo.domain.shop.dto;

import com.example.demo.domain.shop.entity.Inventory;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PurchaseHistoryDto {

    private Long inventoryId;
    private Long emoteId;
    private String emoteName;
    private String emoteImageUrl;
    private int pricePaid;
    private LocalDateTime purchasedAt;
    private Integer pointBalance; // 구매 후 잔여 포인트

    public static PurchaseHistoryDto from(Inventory inventory) {
        return new PurchaseHistoryDto(
                inventory.getId(),
                inventory.getEmote().getId(),
                inventory.getEmote().getName(),
                inventory.getEmote().getImageUrl(),
                inventory.getEmote().getPrice(),
                inventory.getPurchasedAt(),
                null
        );
    }

    public static PurchaseHistoryDto fromWithBalance(Inventory inventory, Integer pointBalance) {
        return new PurchaseHistoryDto(
                inventory.getId(),
                inventory.getEmote().getId(),
                inventory.getEmote().getName(),
                inventory.getEmote().getImageUrl(),
                inventory.getEmote().getPrice(),
                inventory.getPurchasedAt(),
                pointBalance
        );
    }
}
