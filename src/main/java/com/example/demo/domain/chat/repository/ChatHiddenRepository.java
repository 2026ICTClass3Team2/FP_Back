package com.example.demo.domain.chat.repository;

import com.example.demo.domain.chat.entity.ChatHidden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ChatHiddenRepository extends JpaRepository<ChatHidden, Long> {

    List<ChatHidden> findByUserId(Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ChatHidden c WHERE c.userId = :userId AND c.partnerId = :partnerId")
    void deleteByUserIdAndPartnerId(@Param("userId") Long userId, @Param("partnerId") Long partnerId);
}
