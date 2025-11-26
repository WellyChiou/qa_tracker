package com.example.helloworld.service;

import com.example.helloworld.entity.UrlPermission;
import com.example.helloworld.repository.UrlPermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UrlPermissionService {

    @Autowired
    private UrlPermissionRepository urlPermissionRepository;

    /**
     * 獲取所有啟用的 URL 權限配置（按順序排序）
     */
    @Cacheable(value = "urlPermissions")
    @Transactional(readOnly = true)
    public List<UrlPermission> getAllActivePermissions() {
        return urlPermissionRepository.findActivePermissionsOrdered();
    }

    /**
     * 獲取所有 URL 權限配置
     */
    @Transactional(readOnly = true)
    public List<UrlPermission> getAllPermissions() {
        return urlPermissionRepository.findAll();
    }

    /**
     * 根據 ID 獲取 URL 權限配置
     */
    @Transactional(readOnly = true)
    public Optional<UrlPermission> getPermissionById(Long id) {
        return urlPermissionRepository.findById(id);
    }

    /**
     * 創建 URL 權限配置
     */
    @CacheEvict(value = "urlPermissions", allEntries = true)
    @Transactional
    public UrlPermission createPermission(UrlPermission permission) {
        return urlPermissionRepository.save(permission);
    }

    /**
     * 更新 URL 權限配置
     */
    @CacheEvict(value = "urlPermissions", allEntries = true)
    @Transactional
    public UrlPermission updatePermission(Long id, UrlPermission permission) {
        UrlPermission existing = urlPermissionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("URL 權限配置不存在: " + id));
        
        existing.setUrlPattern(permission.getUrlPattern());
        existing.setHttpMethod(permission.getHttpMethod());
        existing.setRequiredRole(permission.getRequiredRole());
        existing.setRequiredPermission(permission.getRequiredPermission());
        existing.setIsPublic(permission.getIsPublic());
        existing.setOrderIndex(permission.getOrderIndex());
        existing.setIsActive(permission.getIsActive());
        existing.setDescription(permission.getDescription());
        
        return urlPermissionRepository.save(existing);
    }

    /**
     * 刪除 URL 權限配置
     */
    @CacheEvict(value = "urlPermissions", allEntries = true)
    @Transactional
    public void deletePermission(Long id) {
        urlPermissionRepository.deleteById(id);
    }

    /**
     * 根據 URL 模式查找匹配的權限配置
     */
    @Transactional(readOnly = true)
    public Optional<UrlPermission> findByUrlPattern(String urlPattern) {
        return urlPermissionRepository.findByUrlPattern(urlPattern);
    }
}

