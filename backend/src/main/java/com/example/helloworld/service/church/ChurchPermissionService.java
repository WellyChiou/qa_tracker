package com.example.helloworld.service.church;

import com.example.helloworld.entity.church.ChurchPermission;
import com.example.helloworld.repository.church.ChurchPermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ChurchPermissionService {

    @Autowired
    private ChurchPermissionRepository churchPermissionRepository;

    /**
     * 獲取所有權限
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public List<ChurchPermission> getAllPermissions() {
        return churchPermissionRepository.findAll();
    }
    
    /**
     * 獲取所有權限（支持分頁和過濾）
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public Page<ChurchPermission> getAllPermissions(String permissionCode, String resource, String action, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String filterPermissionCode = (permissionCode != null && !permissionCode.trim().isEmpty()) ? permissionCode.trim() : null;
        String filterResource = (resource != null && !resource.trim().isEmpty()) ? resource.trim() : null;
        String filterAction = (action != null && !action.trim().isEmpty()) ? action.trim() : null;
        return churchPermissionRepository.findByFilters(filterPermissionCode, filterResource, filterAction, pageable);
    }

    /**
     * 根據 ID 獲取權限
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public Optional<ChurchPermission> getPermissionById(Long id) {
        return churchPermissionRepository.findById(id);
    }

    /**
     * 根據權限代碼獲取權限
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public Optional<ChurchPermission> getPermissionByCode(String permissionCode) {
        return churchPermissionRepository.findByPermissionCode(permissionCode);
    }

    /**
     * 根據資源獲取權限
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public List<ChurchPermission> getPermissionsByResource(String resource) {
        return churchPermissionRepository.findByResource(resource);
    }

    /**
     * 創建權限
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public ChurchPermission createPermission(ChurchPermission permission) {
        // 檢查權限代碼是否已存在
        if (churchPermissionRepository.findByPermissionCode(permission.getPermissionCode()).isPresent()) {
            throw new RuntimeException("權限代碼已存在: " + permission.getPermissionCode());
        }
        return churchPermissionRepository.save(permission);
    }

    /**
     * 更新權限
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public ChurchPermission updatePermission(Long id, ChurchPermission permissionUpdate) {
        ChurchPermission existing = churchPermissionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("權限不存在: " + id));

        // 更新基本信息
        if (permissionUpdate.getPermissionCode() != null && 
            !permissionUpdate.getPermissionCode().equals(existing.getPermissionCode())) {
            // 檢查新權限代碼是否已存在
            if (churchPermissionRepository.findByPermissionCode(permissionUpdate.getPermissionCode()).isPresent()) {
                throw new RuntimeException("權限代碼已存在: " + permissionUpdate.getPermissionCode());
            }
            existing.setPermissionCode(permissionUpdate.getPermissionCode());
        }
        if (permissionUpdate.getPermissionName() != null) {
            existing.setPermissionName(permissionUpdate.getPermissionName());
        }
        if (permissionUpdate.getResource() != null) {
            existing.setResource(permissionUpdate.getResource());
        }
        if (permissionUpdate.getAction() != null) {
            existing.setAction(permissionUpdate.getAction());
        }
        if (permissionUpdate.getDescription() != null) {
            existing.setDescription(permissionUpdate.getDescription());
        }

        return churchPermissionRepository.save(existing);
    }

    /**
     * 刪除權限
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public void deletePermission(Long id) {
        churchPermissionRepository.deleteById(id);
    }
}

