package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.SundayMessage;
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
public interface SundayMessageRepository extends JpaRepository<SundayMessage, Long> {
    List<SundayMessage> findByIsActiveTrueOrderByServiceDateDesc();
    List<SundayMessage> findAllByOrderByServiceDateDesc();
    Optional<SundayMessage> findByServiceDateAndServiceType(LocalDate serviceDate, String serviceType);
    
    // 支持分頁和過濾的查詢
    @Query("SELECT m FROM SundayMessage m WHERE " +
           "(:title IS NULL OR :title = '' OR LOWER(m.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
           "(:startDate IS NULL OR m.serviceDate >= :startDate) AND " +
           "(:endDate IS NULL OR m.serviceDate <= :endDate) AND " +
           "(:serviceType IS NULL OR :serviceType = '' OR m.serviceType = :serviceType) AND " +
           "(:isActive IS NULL OR m.isActive = :isActive) " +
           "ORDER BY m.serviceDate DESC")
    Page<SundayMessage> findByFilters(
        @Param("title") String title,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("serviceType") String serviceType,
        @Param("isActive") Boolean isActive,
        Pageable pageable
    );
}

