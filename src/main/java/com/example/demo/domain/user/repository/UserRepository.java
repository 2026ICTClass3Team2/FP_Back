package com.example.demo.domain.user.repository;

import com.example.demo.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("select u from User u left join fetch u.roleList where u.email = :email")
    Optional<User> getWithRoles(@Param("email") String email);

    boolean existsByEmail(String email);

    // OAuth 로그인을 위한 조회 (provider + providerId 조합으로 찾거나 email로 찾음)
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
    
    // 이메일로 유저를 찾는 일반적인 Optional 조회
    Optional<User> findByEmail(String email);
}
