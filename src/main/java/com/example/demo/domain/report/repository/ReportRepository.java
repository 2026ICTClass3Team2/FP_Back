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
    List<Report> findAllByOrderByCreatedAtDesc();
    List<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status);

    @Modifying
    @Query("UPDATE Report r SET r.status = :status WHERE r.id = :reportId")
    void updateStatus(@Param("reportId") Long reportId, @Param("status") ReportStatus status);
}
