package com.example.helloworld.repository.invest.auth;

import com.example.helloworld.entity.invest.auth.InvestUrlPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvestUrlPermissionRepository extends JpaRepository<InvestUrlPermission, Long> {
    @Query("SELECT u FROM InvestUrlPermission u WHERE u.isActive = true ORDER BY u.orderIndex ASC, u.id ASC")
    List<InvestUrlPermission> findActivePermissionsOrdered();

    @Query("SELECT u FROM InvestUrlPermission u WHERE " +
        "(:urlPattern IS NULL OR LOWER(u.urlPattern) LIKE LOWER(CONCAT('%', :urlPattern, '%'))) AND " +
        "(:httpMethod IS NULL OR u.httpMethod = :httpMethod) AND " +
        "(:isPublic IS NULL OR u.isPublic = :isPublic) AND " +
        "(:requiredPermission IS NULL OR LOWER(u.requiredPermission) LIKE LOWER(CONCAT('%', :requiredPermission, '%'))) AND " +
        "(:isActive IS NULL OR u.isActive = :isActive) " +
        "ORDER BY u.orderIndex ASC, u.id ASC")
    Page<InvestUrlPermission> findByFilters(
        @Param("urlPattern") String urlPattern,
        @Param("httpMethod") String httpMethod,
        @Param("isPublic") Boolean isPublic,
        @Param("requiredPermission") String requiredPermission,
        @Param("isActive") Boolean isActive,
        Pageable pageable
    );

    @Query("SELECT u FROM InvestUrlPermission u WHERE u.urlPattern = :urlPattern " +
        "AND ((u.httpMethod IS NULL AND :httpMethod IS NULL) OR u.httpMethod = :httpMethod)")
    Optional<InvestUrlPermission> findByUrlPatternAndHttpMethod(
        @Param("urlPattern") String urlPattern,
        @Param("httpMethod") String httpMethod
    );
}
