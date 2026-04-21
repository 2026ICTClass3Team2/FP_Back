package com.example.demo.domain.shop.service;

import com.example.demo.domain.shop.entity.Emote;
import com.example.demo.domain.shop.repository.ShopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShopService {
    private final ShopRepository shopRepository;

    public ShopService(ShopRepository shopRepository) {
        this.shopRepository = shopRepository;
    }

    @Transactional
    public Long register (Emote item){
        return shopRepository.save(item).getId();
    }
}
