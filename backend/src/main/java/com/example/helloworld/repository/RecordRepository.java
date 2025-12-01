package com.example.helloworld.repository;

import com.example.helloworld.entity.Record;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecordRepository extends JpaRepository<Record, Long> {
    
    // 根據 Firebase ID 查找（遷移用）
    Optional<Record> findByFirebaseId(String firebaseId);
    
    // 根據 Issue Number 查找
    List<Record> findByIssueNumber(Integer issueNumber);
    
    // 根據狀態查找
    List<Record> findByStatus(Integer status);
    
    // 根據建立者查找
    List<Record> findByCreatedByUid(String createdByUid);
    
    // 複合查詢
    @Query("SELECT r FROM Record r WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:category IS NULL OR r.category = :category) AND " +
           "(:testPlan IS NULL OR r.testPlan = :testPlan) AND " +
           "(:bugFound IS NULL OR r.bugFound = :bugFound) AND " +
           "(:issueNumber IS NULL OR r.issueNumber = :issueNumber) AND " +
           "(:keyword IS NULL OR r.feature LIKE CONCAT('%', :keyword, '%') OR r.memo LIKE CONCAT('%', :keyword, '%') OR CAST(r.issueNumber AS string) LIKE CONCAT('%', :keyword, '%')) AND " +
           "(:testStartDateFrom IS NULL OR r.testStartDate >= :testStartDateFrom) AND " +
           "(:testStartDateTo IS NULL OR r.testStartDate <= :testStartDateTo) AND " +
           "(:etaDateFrom IS NULL OR r.etaDate >= :etaDateFrom) AND " +
           "(:etaDateTo IS NULL OR r.etaDate <= :etaDateTo)")
    Page<Record> searchRecords(
        @Param("status") Integer status,
        @Param("category") Integer category,
        @Param("testPlan") String testPlan,
        @Param("bugFound") Integer bugFound,
        @Param("issueNumber") Integer issueNumber,
        @Param("keyword") String keyword,
        @Param("testStartDateFrom") LocalDate testStartDateFrom,
        @Param("testStartDateTo") LocalDate testStartDateTo,
        @Param("etaDateFrom") LocalDate etaDateFrom,
        @Param("etaDateTo") LocalDate etaDateTo,
        Pageable pageable
    );
    
    // 統計執行中筆數
    long countByStatus(Integer status);

    // 統計指定年份的總記錄數（開始測試日期）
    @Query("SELECT COUNT(r) FROM Record r WHERE YEAR(r.testStartDate) = :year")
    long countByTestStartDateYear(@Param("year") int year);

    // 統計指定年份和狀態的記錄數
    @Query("SELECT COUNT(r) FROM Record r WHERE YEAR(r.testStartDate) = :year AND r.status = :status")
    long countByTestStartDateYearAndStatus(@Param("year") int year, @Param("status") int status);
    
    // 統計符合查詢條件的記錄中各種狀態的數量
    @Query("SELECT COUNT(r) FROM Record r WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:category IS NULL OR r.category = :category) AND " +
           "(:testPlan IS NULL OR r.testPlan = :testPlan) AND " +
           "(:bugFound IS NULL OR r.bugFound = :bugFound) AND " +
           "(:issueNumber IS NULL OR r.issueNumber = :issueNumber) AND " +
           "(:keyword IS NULL OR r.feature LIKE CONCAT('%', :keyword, '%') OR r.memo LIKE CONCAT('%', :keyword, '%') OR CAST(r.issueNumber AS string) LIKE CONCAT('%', :keyword, '%')) AND " +
           "(:testStartDateFrom IS NULL OR r.testStartDate >= :testStartDateFrom) AND " +
           "(:testStartDateTo IS NULL OR r.testStartDate <= :testStartDateTo) AND " +
           "(:etaDateFrom IS NULL OR r.etaDate >= :etaDateFrom) AND " +
           "(:etaDateTo IS NULL OR r.etaDate <= :etaDateTo) AND " +
           "r.status = :targetStatus")
    long countBySearchConditionsAndStatus(
        @Param("status") Integer status,
        @Param("category") Integer category,
        @Param("testPlan") String testPlan,
        @Param("bugFound") Integer bugFound,
        @Param("issueNumber") Integer issueNumber,
        @Param("keyword") String keyword,
        @Param("testStartDateFrom") LocalDate testStartDateFrom,
        @Param("testStartDateTo") LocalDate testStartDateTo,
        @Param("etaDateFrom") LocalDate etaDateFrom,
        @Param("etaDateTo") LocalDate etaDateTo,
        @Param("targetStatus") Integer targetStatus
    );
}

