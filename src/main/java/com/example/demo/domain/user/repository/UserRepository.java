package com.example.demo.domain.user.repository;

import com.example.demo.domain.user.entity.Provider;
import com.example.demo.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.demo.domain.user.entity.UserStatus;
import java.time.LocalDateTime;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByNickname(String nickname);
    
    Optional<User> findByNickname(String nickname);

    // OAuth 로그인을 위한 조회 (provider + providerId 조합으로 찾거나 email로 찾음)
    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);
    
    // 이메일로 유저를 찾는 일반적인 Optional 조회
    Optional<User> findByEmail(String email);

    // 아이디(username)로 유저를 찾는 Optional 조회
    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR u.nickname LIKE %:keyword% OR u.username LIKE %:keyword% OR u.email LIKE %:keyword%) " +
           "AND (:status IS NULL OR u.status = :status)")
    Page<User> searchUsers(@Param("keyword") String keyword, @Param("status") UserStatus status, Pageable pageable);

    List<User> findByStatusAndUpdatedAtBefore(UserStatus status, LocalDateTime dateTime);

    List<User> findTop5ByNicknameContainingOrUsernameContaining(String nickname, String username);
}
