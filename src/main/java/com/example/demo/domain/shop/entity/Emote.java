package com.example.demo.domain.shop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@NoArgsConstructor
public class Emote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "emote_id")
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;  // 이모티콘 이름

    @Column(name = "price", nullable = false)
    private int price;      // 가격 (코인)

    @Column(name = "image_url", nullable = false)
    private String imageUrl; // 이미지 경로


}
