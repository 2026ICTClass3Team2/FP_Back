package com.example.demo.domain.point.repository;

import com.example.demo.domain.point.entity.PointTransaction;
import com.example.demo.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    // 포인트 내역 전체 조회 (최신순)
    Page<PointTransaction> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // 구매 당시 잔여 포인트 조회용
    Optional<PointTransaction> findByUserAndTargetIdAndTargetType(User user, Long targetId, String targetType);
}
