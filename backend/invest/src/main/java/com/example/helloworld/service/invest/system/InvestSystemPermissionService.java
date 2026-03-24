package com.example.helloworld.service.invest.system;

import com.example.helloworld.dto.invest.SystemPermissionDto;
import com.example.helloworld.dto.invest.SystemPermissionRoleBindingDto;
import com.example.helloworld.dto.invest.SystemPermissionUpsertRequestDto;
import com.example.helloworld.entity.invest.auth.InvestPermission;
import com.example.helloworld.repository.invest.auth.InvestPermissionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(transactionManager = "investTransactionManager")
public class InvestSystemPermissionService {

    private final InvestPermissionRepository investPermissionRepository;

    public InvestSystemPermissionService(InvestPermissionRepository investPermissionRepository) {
        this.investPermissionRepository = investPermissionRepository;
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public Page<SystemPermissionDto> getPaged(String permissionCode, String resource, String action, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return investPermissionRepository.findByFilters(normalize(permissionCode), normalize(resource), normalize(action), pageable)
            .map(this::toPagedDto);
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public SystemPermissionDto getDetail(Long id) {
        InvestPermission permission = getPermission(id);
        return toDetailDto(permission);
    }

    public SystemPermissionDto create(SystemPermissionUpsertRequestDto request) {
        String permissionCode = require(request.getPermissionCode(), "permissionCode");
        String permissionName = require(request.getPermissionName(), "permissionName");

        ensurePermissionCodeAvailable(permissionCode, null);

        InvestPermission permission = new InvestPermission();
        permission.setPermissionCode(permissionCode);
        permission.setPermissionName(permissionName);
        permission.setResource(normalize(request.getResource()));
        permission.setAction(normalize(request.getAction()));
        permission.setDescription(normalize(request.getDescription()));

        InvestPermission saved = investPermissionRepository.save(permission);
        return getDetail(saved.getId());
    }

    public SystemPermissionDto update(Long id, SystemPermissionUpsertRequestDto request) {
        InvestPermission permission = getPermission(id);

        String permissionCode = require(request.getPermissionCode(), "permissionCode");
        String permissionName = require(request.getPermissionName(), "permissionName");

        ensurePermissionCodeAvailable(permissionCode, id);

        permission.setPermissionCode(permissionCode);
        permission.setPermissionName(permissionName);
        permission.setResource(normalize(request.getResource()));
        permission.setAction(normalize(request.getAction()));
        permission.setDescription(normalize(request.getDescription()));

        investPermissionRepository.save(permission);
        return getDetail(id);
    }

    public void delete(Long id) {
        InvestPermission permission = getPermission(id);

        if (permission.getPermissionCode() != null && permission.getPermissionCode().startsWith("INVEST_SYS_")) {
            throw new RuntimeException("系統設定核心權限不可刪除：" + permission.getPermissionCode());
        }

        long roleBinding = investPermissionRepository.countRoleBindings(permission.getId());
        long userBinding = investPermissionRepository.countUserBindings(permission.getId());
        long menuBinding = investPermissionRepository.countMenuBindings(permission.getPermissionCode());
        long urlBinding = investPermissionRepository.countUrlBindings(permission.getPermissionCode());

        if (roleBinding > 0 || userBinding > 0 || menuBinding > 0 || urlBinding > 0) {
            throw new RuntimeException("此權限仍有綁定，無法刪除（role=" + roleBinding + ", user=" + userBinding + ", menu=" + menuBinding + ", url=" + urlBinding + "）");
        }

        investPermissionRepository.delete(permission);
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public List<SystemPermissionRoleBindingDto> getRoleBindings(Long permissionId) {
        getPermission(permissionId);
        return investPermissionRepository.findRoleBindings(permissionId).stream()
            .map(view -> {
                SystemPermissionRoleBindingDto dto = new SystemPermissionRoleBindingDto();
                dto.setRoleId(view.getRoleId());
                dto.setRoleName(view.getRoleName());
                return dto;
            })
            .toList();
    }

    private SystemPermissionDto toPagedDto(InvestPermission permission) {
        SystemPermissionDto dto = new SystemPermissionDto();
        dto.setId(permission.getId());
        dto.setPermissionCode(permission.getPermissionCode());
        dto.setPermissionName(permission.getPermissionName());
        dto.setResource(permission.getResource());
        dto.setAction(permission.getAction());
        dto.setDescription(permission.getDescription());
        dto.setRoleCount(Math.toIntExact(investPermissionRepository.countRoleBindings(permission.getId())));
        dto.setCreatedAt(permission.getCreatedAt());
        dto.setUpdatedAt(permission.getUpdatedAt());
        return dto;
    }

    private SystemPermissionDto toDetailDto(InvestPermission permission) {
        SystemPermissionDto dto = toPagedDto(permission);
        dto.setRoleBindings(getRoleBindings(permission.getId()));
        return dto;
    }

    private InvestPermission getPermission(Long id) {
        return investPermissionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("找不到權限，id=" + id));
    }

    private void ensurePermissionCodeAvailable(String permissionCode, Long currentPermissionId) {
        investPermissionRepository.findByPermissionCode(permissionCode).ifPresent(existing -> {
            if (currentPermissionId == null || !Objects.equals(existing.getId(), currentPermissionId)) {
                throw new RuntimeException("權限代碼已存在：" + permissionCode);
            }
        });
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String require(String value, String fieldName) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new RuntimeException(fieldName + " 為必填");
        }
        return normalized;
    }
}
