package com.example.helloworld.repository.personal;

import com.example.helloworld.entity.personal.Permission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByPermissionCode(String permissionCode);
    List<Permission> findByResource(String resource);
    List<Permission> findByAction(String action);

    @Query("SELECT p FROM Permission p WHERE " +
           "(:permissionCode IS NULL OR :permissionCode = '' OR LOWER(p.permissionCode) LIKE LOWER(CONCAT('%', :permissionCode, '%'))) AND " +
           "(:resource IS NULL OR :resource = '' OR LOWER(p.resource) LIKE LOWER(CONCAT('%', :resource, '%'))) AND " +
           "(:action IS NULL OR :action = '' OR LOWER(p.action) LIKE LOWER(CONCAT('%', :action, '%'))) " +
           "ORDER BY p.id")
    Page<Permission> findByFilters(
        @Param("permissionCode") String permissionCode,
        @Param("resource") String resource,
        @Param("action") String action,
        Pageable pageable
    );
}
