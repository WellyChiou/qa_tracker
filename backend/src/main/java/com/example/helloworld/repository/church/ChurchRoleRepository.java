package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.ChurchRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChurchRoleRepository extends JpaRepository<ChurchRole, Long> {
    Optional<ChurchRole> findByRoleName(String roleName);
    
    @Query("SELECT DISTINCT r FROM ChurchRole r LEFT JOIN FETCH r.permissions")
    List<ChurchRole> findAllWithPermissions();
    
    @Query("SELECT DISTINCT r FROM ChurchRole r LEFT JOIN FETCH r.permissions WHERE r.id = :id")
    Optional<ChurchRole> findByIdWithPermissions(@Param("id") Long id);
    
    // 支持分頁和過濾的查詢
    @Query("SELECT DISTINCT r FROM ChurchRole r LEFT JOIN FETCH r.permissions WHERE (:roleName IS NULL OR :roleName = '' OR LOWER(r.roleName) LIKE LOWER(CONCAT('%', :roleName, '%')))")
    Page<ChurchRole> findByFilters(@Param("roleName") String roleName, Pageable pageable);
}

