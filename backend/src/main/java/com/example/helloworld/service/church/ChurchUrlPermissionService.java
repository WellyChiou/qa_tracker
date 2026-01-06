package com.example.helloworld.service.church;

import com.example.helloworld.entity.church.ChurchUrlPermission;
import com.example.helloworld.repository.church.ChurchUrlPermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ChurchUrlPermissionService {

    @Autowired
    private ChurchUrlPermissionRepository churchUrlPermissionRepository;

    /**
     * 獲取所有啟用的 URL 權限配置（按順序排序）
     */
    // @Cacheable(value = "churchUrlPermissions") // 暫時禁用緩存以便調試
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public List<ChurchUrlPermission> getAllActivePermissions() {
        return churchUrlPermissionRepository.findActivePermissionsOrdered();
    }

    /**
     * 獲取所有 URL 權限配置
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public List<ChurchUrlPermission> getAllPermissions() {
        return churchUrlPermissionRepository.findAll();
    }
    
    /**
     * 獲取所有 URL 權限配置（支持分頁和過濾）
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public Page<ChurchUrlPermission> getAllPermissions(
            String urlPattern, String httpMethod, Boolean isPublic, String requiredPermission, Boolean isActive,
            int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String filterUrlPattern = (urlPattern != null && !urlPattern.trim().isEmpty()) ? urlPattern.trim() : null;
        String filterHttpMethod = (httpMethod != null && !httpMethod.trim().isEmpty()) ? httpMethod.trim() : null;
        String filterRequiredPermission = (requiredPermission != null && !requiredPermission.trim().isEmpty()) ? requiredPermission.trim() : null;
        return churchUrlPermissionRepository.findByFilters(filterUrlPattern, filterHttpMethod, isPublic, filterRequiredPermission, isActive, pageable);
    }

    /**
     * 根據 ID 獲取 URL 權限配置
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public Optional<ChurchUrlPermission> getPermissionById(Long id) {
        return churchUrlPermissionRepository.findById(id);
    }

    /**
     * 創建 URL 權限配置
     */
    @CacheEvict(value = "churchUrlPermissions", allEntries = true)
    @Transactional(transactionManager = "churchTransactionManager")
    public ChurchUrlPermission createPermission(ChurchUrlPermission permission) {
        return churchUrlPermissionRepository.save(permission);
    }

    /**
     * 更新 URL 權限配置
     */
    @CacheEvict(value = "churchUrlPermissions", allEntries = true)
    @Transactional(transactionManager = "churchTransactionManager")
    public ChurchUrlPermission updatePermission(Long id, ChurchUrlPermission permission) {
        ChurchUrlPermission existing = churchUrlPermissionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("URL 權限配置不存在: " + id));
        
        existing.setUrlPattern(permission.getUrlPattern());
        existing.setHttpMethod(permission.getHttpMethod());
        existing.setRequiredRole(permission.getRequiredRole());
        existing.setRequiredPermission(permission.getRequiredPermission());
        existing.setIsPublic(permission.getIsPublic());
        existing.setOrderIndex(permission.getOrderIndex());
        existing.setIsActive(permission.getIsActive());
        existing.setDescription(permission.getDescription());
        
        return churchUrlPermissionRepository.save(existing);
    }

    /**
     * 刪除 URL 權限配置
     */
    @CacheEvict(value = "churchUrlPermissions", allEntries = true)
    @Transactional(transactionManager = "churchTransactionManager")
    public void deletePermission(Long id) {
        churchUrlPermissionRepository.deleteById(id);
    }

    /**
     * 根據 URL 模式查找匹配的權限配置
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public Optional<ChurchUrlPermission> findByUrlPattern(String urlPattern) {
        return Optional.ofNullable(churchUrlPermissionRepository.findByUrlPattern(urlPattern));
    }
}

