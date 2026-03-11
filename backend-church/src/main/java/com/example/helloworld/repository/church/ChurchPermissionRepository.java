package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.ChurchPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChurchPermissionRepository extends JpaRepository<ChurchPermission, Long> {
    Optional<ChurchPermission> findByPermissionCode(String permissionCode);
    
    @Query("SELECT p FROM ChurchPermission p WHERE p.resource = :resource")
    List<ChurchPermission> findByResource(@Param("resource") String resource);
    
    // 支持分頁和過濾的查詢
    @Query("SELECT p FROM ChurchPermission p WHERE " +
           "(:permissionCode IS NULL OR :permissionCode = '' OR LOWER(p.permissionCode) LIKE LOWER(CONCAT('%', :permissionCode, '%'))) AND " +
           "(:resource IS NULL OR :resource = '' OR LOWER(p.resource) LIKE LOWER(CONCAT('%', :resource, '%'))) AND " +
           "(:action IS NULL OR :action = '' OR LOWER(p.action) LIKE LOWER(CONCAT('%', :action, '%'))) " +
           "ORDER BY p.resource, p.action")
    Page<ChurchPermission> findByFilters(
        @Param("permissionCode") String permissionCode,
        @Param("resource") String resource,
        @Param("action") String action,
        Pageable pageable
    );
}

