package com.example.helloworld.repository;

import com.example.helloworld.entity.Record;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
           "(:keyword IS NULL OR r.feature LIKE %:keyword% OR r.memo LIKE %:keyword%)")
    Page<Record> searchRecords(
        @Param("status") Integer status,
        @Param("category") Integer category,
        @Param("testPlan") String testPlan,
        @Param("bugFound") Integer bugFound,
        @Param("issueNumber") Integer issueNumber,
        @Param("keyword") String keyword,
        Pageable pageable
    );
    
    // 統計執行中筆數
    long countByStatus(Integer status);
}

