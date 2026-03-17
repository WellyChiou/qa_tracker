package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.Position;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {
    Optional<Position> findByPositionCode(String positionCode);
    
    Optional<Position> findByPositionName(String positionName);
    
    List<Position> findByIsActiveTrueOrderBySortOrderAsc();
    
    List<Position> findAllByOrderBySortOrderAsc();
    
    // 支持分頁和過濾的查詢
    @Query("SELECT p FROM Position p WHERE " +
           "(:positionCode IS NULL OR :positionCode = '' OR LOWER(p.positionCode) LIKE LOWER(CONCAT('%', :positionCode, '%'))) AND " +
           "(:positionName IS NULL OR :positionName = '' OR LOWER(p.positionName) LIKE LOWER(CONCAT('%', :positionName, '%'))) AND " +
           "(:isActive IS NULL OR p.isActive = :isActive) " +
           "ORDER BY p.sortOrder, p.id")
    Page<Position> findByFilters(
        @Param("positionCode") String positionCode,
        @Param("positionName") String positionName,
        @Param("isActive") Boolean isActive,
        Pageable pageable
    );
}
