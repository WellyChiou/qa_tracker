package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.ChurchUrlPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}

