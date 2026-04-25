package com.example.demo.domain.shop.repository;

import com.example.demo.domain.shop.entity.Emote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ShopRepository extends JpaRepository<Emote, Long> {

    boolean existsByName(String name);
    
    @Query("SELECT e FROM Emote e WHERE e.id NOT IN " +
           "(SELECT i.emote.id FROM Inventory i WHERE i.user.id = :userId)")
    Page<Emote> findUnpurchasedByUser(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT e FROM Emote e WHERE e.id IN " +
           "(SELECT i.emote.id FROM Inventory i WHERE i.user.id = :userId)")
    Page<Emote> findPurchasedByUser(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT e.id FROM Emote e JOIN Inventory i ON i.emote.id = e.id WHERE i.user.id = :userId")
    List<Long> findPurchasedEmoteIdsByUser(@Param("userId") Long userId);
}
