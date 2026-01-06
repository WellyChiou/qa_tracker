package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.ChurchMenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChurchMenuItemRepository extends JpaRepository<ChurchMenuItem, Long> {
    Optional<ChurchMenuItem> findByMenuCode(String menuCode);
    
    @Query("SELECT m FROM ChurchMenuItem m WHERE m.isActive = true AND m.parentId IS NULL AND m.menuType = :menuType ORDER BY m.orderIndex ASC")
    List<ChurchMenuItem> findActiveRootMenusByType(@Param("menuType") String menuType);
    
    @Query("SELECT m FROM ChurchMenuItem m WHERE m.isActive = true AND m.parentId = :parentId ORDER BY m.orderIndex ASC")
    List<ChurchMenuItem> findActiveChildMenus(@Param("parentId") Long parentId);
    
    @Query("SELECT m FROM ChurchMenuItem m WHERE m.parentId = :parentId ORDER BY m.orderIndex ASC")
    List<ChurchMenuItem> findByParentIdOrderByOrderIndexAsc(@Param("parentId") Long parentId);
    
    @Query("SELECT m FROM ChurchMenuItem m WHERE m.menuType = :menuType AND m.isActive = :isActive AND m.showInDashboard = :showInDashboard AND m.parentId IS NULL ORDER BY m.orderIndex ASC")
    List<ChurchMenuItem> findByMenuTypeAndIsActiveAndShowInDashboardAndParentIdIsNullOrderByOrderIndexAsc(
        @Param("menuType") String menuType,
        @Param("isActive") Boolean isActive,
        @Param("showInDashboard") Boolean showInDashboard
    );
    
    // 支持分頁和過濾的查詢
    @Query("SELECT m FROM ChurchMenuItem m WHERE " +
           "(:menuCode IS NULL OR :menuCode = '' OR LOWER(m.menuCode) LIKE LOWER(CONCAT('%', :menuCode, '%'))) AND " +
           "(:menuName IS NULL OR :menuName = '' OR LOWER(m.menuName) LIKE LOWER(CONCAT('%', :menuName, '%'))) AND " +
           "(:menuType IS NULL OR :menuType = '' OR m.menuType = :menuType) AND " +
           "(:isActive IS NULL OR m.isActive = :isActive) " +
           "ORDER BY m.orderIndex")
    Page<ChurchMenuItem> findByFilters(
        @Param("menuCode") String menuCode,
        @Param("menuName") String menuName,
        @Param("menuType") String menuType,
        @Param("isActive") Boolean isActive,
        Pageable pageable
    );
}

