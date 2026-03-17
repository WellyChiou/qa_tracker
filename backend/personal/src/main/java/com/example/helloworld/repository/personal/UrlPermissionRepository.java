package com.example.helloworld.repository.personal;

import com.example.helloworld.entity.personal.UrlPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UrlPermissionRepository extends JpaRepository<UrlPermission, Long> {
    List<UrlPermission> findByIsActiveTrueOrderByOrderIndexAsc();
    
    Optional<UrlPermission> findByUrlPattern(String urlPattern);
    
    @Query("SELECT u FROM UrlPermission u WHERE u.isActive = true ORDER BY u.orderIndex ASC")
    List<UrlPermission> findActivePermissionsOrdered();

    @Query("SELECT u FROM UrlPermission u WHERE " +
           "(:urlPattern IS NULL OR :urlPattern = '' OR LOWER(u.urlPattern) LIKE LOWER(CONCAT('%', :urlPattern, '%'))) AND " +
           "(:httpMethod IS NULL OR :httpMethod = '' OR u.httpMethod = :httpMethod) AND " +
           "(:isPublic IS NULL OR u.isPublic = :isPublic) AND " +
           "(:requiredPermission IS NULL OR :requiredPermission = '' OR LOWER(u.requiredPermission) LIKE LOWER(CONCAT('%', :requiredPermission, '%'))) AND " +
           "(:isActive IS NULL OR u.isActive = :isActive) " +
           "ORDER BY u.orderIndex, u.id")
    Page<UrlPermission> findByFilters(
        @Param("urlPattern") String urlPattern,
        @Param("httpMethod") String httpMethod,
        @Param("isPublic") Boolean isPublic,
        @Param("requiredPermission") String requiredPermission,
        @Param("isActive") Boolean isActive,
        Pageable pageable
    );
}
