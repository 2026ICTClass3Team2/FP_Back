package com.example.demo.global.jwt.repository;

import com.example.demo.global.jwt.entity.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {
    
    boolean existsByToken(String token);

    @Modifying
    @Transactional
    @Query("DELETE FROM BlacklistedToken b WHERE b.expirationTime < :currentTime")
    void deleteExpiredTokens(Long currentTime);
}
