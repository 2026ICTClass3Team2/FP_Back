package com.example.demo.domain.shop.repository;

import com.example.demo.domain.shop.entity.Emote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ShopRepository extends JpaRepository<Emote,Long> {
}
