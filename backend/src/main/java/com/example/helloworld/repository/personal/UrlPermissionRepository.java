package com.example.helloworld.repository.personal;

import com.example.helloworld.entity.personal.UrlPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UrlPermissionRepository extends JpaRepository<UrlPermission, Long> {
    List<UrlPermission> findByIsActiveTrueOrderByOrderIndexAsc();
    
    Optional<UrlPermission> findByUrlPattern(String urlPattern);
    
    @Query("SELECT u FROM UrlPermission u WHERE u.isActive = true ORDER BY u.orderIndex ASC")
    List<UrlPermission> findActivePermissionsOrdered();
}
