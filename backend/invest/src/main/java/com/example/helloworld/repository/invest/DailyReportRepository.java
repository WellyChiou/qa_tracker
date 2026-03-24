package com.example.helloworld.repository.invest;

import com.example.helloworld.entity.invest.DailyReport;
import com.example.helloworld.entity.invest.DailyReportStatusCode;
import com.example.helloworld.entity.invest.DailyReportTypeCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyReportRepository extends JpaRepository<DailyReport, Long> {

    Optional<DailyReport> findByUserIdAndReportDateAndReportType(String userId, LocalDate reportDate, DailyReportTypeCode reportType);

    Optional<DailyReport> findTopByUserIdAndReportTypeOrderByReportDateDescCreatedAtDesc(String userId, DailyReportTypeCode reportType);

    Optional<DailyReport> findByIdAndUserId(Long id, String userId);

    @Query("SELECT r FROM DailyReport r WHERE " +
        "r.userId = :userId AND " +
        "(:reportDateFrom IS NULL OR r.reportDate >= :reportDateFrom) AND " +
        "(:reportDateTo IS NULL OR r.reportDate <= :reportDateTo) AND " +
        "(:reportType IS NULL OR r.reportType = :reportType) AND " +
        "(:status IS NULL OR r.status = :status) " +
        "ORDER BY r.reportDate DESC, r.id DESC")
    Page<DailyReport> findByUserAndFilters(
        @Param("userId") String userId,
        @Param("reportDateFrom") LocalDate reportDateFrom,
        @Param("reportDateTo") LocalDate reportDateTo,
        @Param("reportType") DailyReportTypeCode reportType,
        @Param("status") DailyReportStatusCode status,
        Pageable pageable
    );
}
