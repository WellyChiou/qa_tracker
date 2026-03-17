package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.AboutInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AboutInfoRepository extends JpaRepository<AboutInfo, Long> {
    List<AboutInfo> findByIsActiveTrueOrderByDisplayOrderAsc();
    List<AboutInfo> findBySectionKeyAndIsActiveTrue(String sectionKey);
    List<AboutInfo> findAllByOrderByDisplayOrderAsc();
    
    // 支持分頁和過濾的查詢
    @Query("SELECT a FROM AboutInfo a WHERE " +
           "(:sectionKey IS NULL OR :sectionKey = '' OR LOWER(a.sectionKey) LIKE LOWER(CONCAT('%', :sectionKey, '%'))) AND " +
           "(:title IS NULL OR :title = '' OR LOWER(a.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
           "(:isActive IS NULL OR a.isActive = :isActive) " +
           "ORDER BY a.displayOrder, a.id")
    Page<AboutInfo> findByFilters(
        @Param("sectionKey") String sectionKey,
        @Param("title") String title,
        @Param("isActive") Boolean isActive,
        Pageable pageable
    );
}

