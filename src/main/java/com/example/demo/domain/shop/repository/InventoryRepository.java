package com.example.demo.domain.shop.repository;

import com.example.demo.domain.shop.entity.Inventory;
import com.example.demo.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    boolean existsByUserAndEmote_Id(User user, Long emoteId);

    Page<Inventory> findByUserOrderByPurchasedAtDesc(User user, Pageable pageable);
}
