package com.example.helloworld.repository.invest.auth;

import com.example.helloworld.entity.invest.auth.InvestPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvestPermissionRepository extends JpaRepository<InvestPermission, Long> {
    List<InvestPermission> findAllByOrderByPermissionCodeAsc();

    Optional<InvestPermission> findByPermissionCode(String permissionCode);

    Optional<InvestPermission> findByPermissionCodeIgnoreCase(String permissionCode);

    boolean existsByPermissionCodeIgnoreCase(String permissionCode);

    @Query("SELECT p FROM InvestPermission p WHERE " +
        "(:permissionCode IS NULL OR LOWER(p.permissionCode) LIKE LOWER(CONCAT('%', :permissionCode, '%'))) AND " +
        "(:resource IS NULL OR LOWER(p.resource) LIKE LOWER(CONCAT('%', :resource, '%'))) AND " +
        "(:action IS NULL OR LOWER(p.action) LIKE LOWER(CONCAT('%', :action, '%'))) " +
        "ORDER BY p.resource, p.action, p.permissionCode")
    Page<InvestPermission> findByFilters(
        @Param("permissionCode") String permissionCode,
        @Param("resource") String resource,
        @Param("action") String action,
        Pageable pageable
    );

    @Query(value = "SELECT COUNT(*) FROM role_permissions rp WHERE rp.permission_id = :permissionId", nativeQuery = true)
    long countRoleBindings(@Param("permissionId") Long permissionId);

    @Query(value = "SELECT COUNT(*) FROM user_permissions up WHERE up.permission_id = :permissionId", nativeQuery = true)
    long countUserBindings(@Param("permissionId") Long permissionId);

    @Query(value = "SELECT COUNT(*) FROM menu_items m WHERE m.required_permission = :permissionCode", nativeQuery = true)
    long countMenuBindings(@Param("permissionCode") String permissionCode);

    @Query(value = "SELECT COUNT(*) FROM url_permissions u WHERE u.required_permission = :permissionCode", nativeQuery = true)
    long countUrlBindings(@Param("permissionCode") String permissionCode);

    @Query(value = "SELECT r.id AS roleId, r.role_name AS roleName " +
        "FROM role_permissions rp " +
        "JOIN roles r ON r.id = rp.role_id " +
        "WHERE rp.permission_id = :permissionId " +
        "ORDER BY r.role_name", nativeQuery = true)
    List<RoleBindingView> findRoleBindings(@Param("permissionId") Long permissionId);

    interface RoleBindingView {
        Long getRoleId();
        String getRoleName();
    }
}
