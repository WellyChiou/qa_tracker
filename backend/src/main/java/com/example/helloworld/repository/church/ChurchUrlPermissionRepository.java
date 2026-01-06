package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.ChurchUrlPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChurchUrlPermissionRepository extends JpaRepository<ChurchUrlPermission, Long> {
    
    /**
     * 獲取所有啟用的 URL 權限配置（按順序排序）
     */
    @Query("SELECT p FROM ChurchUrlPermission p WHERE p.isActive = true ORDER BY p.orderIndex ASC, p.id ASC")
    List<ChurchUrlPermission> findActivePermissionsOrdered();
    
    /**
     * 根據 URL 模式查找
     */
    ChurchUrlPermission findByUrlPattern(String urlPattern);
    
    // 支持分頁和過濾的查詢
    @Query("SELECT p FROM ChurchUrlPermission p WHERE " +
           "(:urlPattern IS NULL OR :urlPattern = '' OR LOWER(p.urlPattern) LIKE LOWER(CONCAT('%', :urlPattern, '%'))) AND " +
           "(:httpMethod IS NULL OR :httpMethod = '' OR p.httpMethod = :httpMethod) AND " +
           "(:isPublic IS NULL OR p.isPublic = :isPublic) AND " +
           "(:requiredPermission IS NULL OR :requiredPermission = '' OR LOWER(p.requiredPermission) LIKE LOWER(CONCAT('%', :requiredPermission, '%'))) AND " +
           "(:isActive IS NULL OR p.isActive = :isActive) " +
           "ORDER BY p.orderIndex, p.id")
    Page<ChurchUrlPermission> findByFilters(
        @Param("urlPattern") String urlPattern,
        @Param("httpMethod") String httpMethod,
        @Param("isPublic") Boolean isPublic,
        @Param("requiredPermission") String requiredPermission,
        @Param("isActive") Boolean isActive,
        Pageable pageable
    );
}

