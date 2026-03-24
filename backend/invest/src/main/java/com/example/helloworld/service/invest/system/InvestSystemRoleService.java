package com.example.helloworld.service.invest.system;

import com.example.helloworld.dto.invest.SystemPermissionOptionDto;
import com.example.helloworld.dto.invest.SystemRoleDto;
import com.example.helloworld.dto.invest.SystemRolePermissionsUpdateRequestDto;
import com.example.helloworld.dto.invest.SystemRoleUpsertRequestDto;
import com.example.helloworld.entity.invest.auth.InvestPermission;
import com.example.helloworld.entity.invest.auth.InvestRole;
import com.example.helloworld.repository.invest.auth.InvestPermissionRepository;
import com.example.helloworld.repository.invest.auth.InvestRoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional(transactionManager = "investTransactionManager")
public class InvestSystemRoleService {

    private static final String SYSTEM_ADMIN_ROLE = "ROLE_INVEST_ADMIN";

    private final InvestRoleRepository investRoleRepository;
    private final InvestPermissionRepository investPermissionRepository;

    public InvestSystemRoleService(
        InvestRoleRepository investRoleRepository,
        InvestPermissionRepository investPermissionRepository
    ) {
        this.investRoleRepository = investRoleRepository;
        this.investPermissionRepository = investPermissionRepository;
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public Page<SystemRoleDto> getPaged(String roleName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return investRoleRepository.findByFilters(normalize(roleName), pageable)
            .map(this::toPagedDto);
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public SystemRoleDto getDetail(Long id) {
        InvestRole role = getRoleWithPermissions(id);
        return toDetailDto(role);
    }

    public SystemRoleDto create(SystemRoleUpsertRequestDto request) {
        String roleName = require(request.getRoleName(), "roleName");
        ensureRoleNameAvailable(roleName, null);

        InvestRole role = new InvestRole();
        role.setRoleName(roleName);
        role.setDescription(normalize(request.getDescription()));
        role.setPermissions(resolvePermissions(request.getPermissionIds()));

        InvestRole saved = investRoleRepository.save(role);
        return getDetail(saved.getId());
    }

    public SystemRoleDto update(Long id, SystemRoleUpsertRequestDto request) {
        InvestRole role = getRoleWithPermissions(id);
        String roleName = require(request.getRoleName(), "roleName");
        ensureRoleNameAvailable(roleName, id);

        role.setRoleName(roleName);
        role.setDescription(normalize(request.getDescription()));
        if (request.getPermissionIds() != null) {
            role.setPermissions(resolvePermissions(request.getPermissionIds()));
        }

        investRoleRepository.save(role);
        return getDetail(id);
    }

    public void delete(Long id) {
        InvestRole role = investRoleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("找不到角色，id=" + id));

        if (SYSTEM_ADMIN_ROLE.equals(role.getRoleName())) {
            throw new RuntimeException("不可刪除系統管理員角色");
        }

        long userBindings = investRoleRepository.countUserBindings(id);
        if (userBindings > 0) {
            throw new RuntimeException("角色仍綁定 " + userBindings + " 位用戶，請先移除綁定後再刪除");
        }

        investRoleRepository.delete(role);
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public List<SystemPermissionOptionDto> getPermissionOptions() {
        return investPermissionRepository.findAllByOrderByPermissionCodeAsc().stream()
            .map(this::toPermissionOption)
            .toList();
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public List<SystemPermissionOptionDto> getRolePermissions(Long roleId) {
        InvestRole role = getRoleWithPermissions(roleId);
        return role.getPermissions().stream()
            .map(this::toPermissionOption)
            .sorted(Comparator.comparing(SystemPermissionOptionDto::getPermissionCode, Comparator.nullsLast(String::compareTo)))
            .toList();
    }

    public List<SystemPermissionOptionDto> updateRolePermissions(Long roleId, SystemRolePermissionsUpdateRequestDto request) {
        InvestRole role = getRoleWithPermissions(roleId);
        role.setPermissions(resolvePermissions(request == null ? null : request.getPermissionIds()));
        investRoleRepository.save(role);
        return getRolePermissions(roleId);
    }

    private SystemRoleDto toPagedDto(InvestRole role) {
        SystemRoleDto dto = new SystemRoleDto();
        dto.setId(role.getId());
        dto.setRoleName(role.getRoleName());
        dto.setDescription(role.getDescription());
        dto.setPermissionCount(Math.toIntExact(investRoleRepository.countPermissionBindings(role.getId())));
        dto.setCreatedAt(role.getCreatedAt());
        dto.setUpdatedAt(role.getUpdatedAt());
        return dto;
    }

    private SystemRoleDto toDetailDto(InvestRole role) {
        SystemRoleDto dto = new SystemRoleDto();
        dto.setId(role.getId());
        dto.setRoleName(role.getRoleName());
        dto.setDescription(role.getDescription());
        List<SystemPermissionOptionDto> permissions = role.getPermissions().stream()
            .map(this::toPermissionOption)
            .sorted(Comparator.comparing(SystemPermissionOptionDto::getPermissionCode, Comparator.nullsLast(String::compareTo)))
            .toList();
        dto.setPermissions(permissions);
        dto.setPermissionCount(permissions.size());
        dto.setCreatedAt(role.getCreatedAt());
        dto.setUpdatedAt(role.getUpdatedAt());
        return dto;
    }

    private SystemPermissionOptionDto toPermissionOption(InvestPermission permission) {
        SystemPermissionOptionDto dto = new SystemPermissionOptionDto();
        dto.setId(permission.getId());
        dto.setPermissionCode(permission.getPermissionCode());
        dto.setPermissionName(permission.getPermissionName());
        dto.setResource(permission.getResource());
        dto.setAction(permission.getAction());
        return dto;
    }

    private Set<InvestPermission> resolvePermissions(List<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return new HashSet<>();
        }

        Set<Long> dedupIds = new LinkedHashSet<>();
        for (Long permissionId : permissionIds) {
            if (permissionId != null) {
                dedupIds.add(permissionId);
            }
        }

        if (dedupIds.isEmpty()) {
            return new HashSet<>();
        }

        List<InvestPermission> permissions = investPermissionRepository.findAllById(dedupIds);
        if (permissions.size() != dedupIds.size()) {
            Set<Long> foundIds = permissions.stream().map(InvestPermission::getId).collect(java.util.stream.Collectors.toSet());
            List<Long> missingIds = new ArrayList<>();
            for (Long permissionId : dedupIds) {
                if (!foundIds.contains(permissionId)) {
                    missingIds.add(permissionId);
                }
            }
            throw new RuntimeException("部分 permission 不存在：" + missingIds);
        }

        return new HashSet<>(permissions);
    }

    private InvestRole getRoleWithPermissions(Long id) {
        return investRoleRepository.findByIdWithPermissions(id)
            .orElseThrow(() -> new RuntimeException("找不到角色，id=" + id));
    }

    private void ensureRoleNameAvailable(String roleName, Long currentRoleId) {
        investRoleRepository.findByRoleName(roleName).ifPresent(existing -> {
            if (currentRoleId == null || !Objects.equals(existing.getId(), currentRoleId)) {
                throw new RuntimeException("角色名稱已存在：" + roleName);
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
