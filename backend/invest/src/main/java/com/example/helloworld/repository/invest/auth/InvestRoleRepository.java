package com.example.helloworld.repository.invest.auth;

import com.example.helloworld.entity.invest.auth.InvestRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvestRoleRepository extends JpaRepository<InvestRole, Long> {
    Optional<InvestRole> findByRoleName(String roleName);

    boolean existsByRoleNameIgnoreCase(String roleName);

    @Query(
        value = "SELECT r FROM InvestRole r " +
            "WHERE (:roleName IS NULL OR LOWER(r.roleName) LIKE LOWER(CONCAT('%', :roleName, '%')))",
        countQuery = "SELECT COUNT(r.id) FROM InvestRole r " +
            "WHERE (:roleName IS NULL OR LOWER(r.roleName) LIKE LOWER(CONCAT('%', :roleName, '%')))"
    )
    Page<InvestRole> findByFilters(@Param("roleName") String roleName, Pageable pageable);

    @Query("SELECT DISTINCT r FROM InvestRole r LEFT JOIN FETCH r.permissions WHERE r.id = :id")
    Optional<InvestRole> findByIdWithPermissions(@Param("id") Long id);

    @Query(value = "SELECT COUNT(*) FROM user_roles ur WHERE ur.role_id = :roleId", nativeQuery = true)
    long countUserBindings(@Param("roleId") Long roleId);

    @Query(value = "SELECT COUNT(*) FROM role_permissions rp WHERE rp.role_id = :roleId", nativeQuery = true)
    long countPermissionBindings(@Param("roleId") Long roleId);
}
