package com.example.demo.domain.report.repository;

import com.example.demo.domain.report.entity.Report;
import com.example.demo.domain.report.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    long countByStatus(ReportStatus status);

    @Query("SELECT r FROM Report r LEFT JOIN FETCH r.reporter ORDER BY r.createdAt DESC")
    List<Report> findAllByOrderByCreatedAtDesc();

    @Query("SELECT r FROM Report r LEFT JOIN FETCH r.reporter WHERE r.status = :status ORDER BY r.createdAt DESC")
    List<Report> findByStatusOrderByCreatedAtDesc(@Param("status") ReportStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Report r SET r.status = :status WHERE r.id = :reportId")
    void updateStatus(@Param("reportId") Long reportId, @Param("status") ReportStatus status);
}
