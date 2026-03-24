package com.example.helloworld.service.invest.system;

import com.example.helloworld.dto.invest.SystemHttpMethodOptionDto;
import com.example.helloworld.dto.invest.SystemPermissionOptionDto;
import com.example.helloworld.dto.invest.SystemUrlPermissionDto;
import com.example.helloworld.dto.invest.SystemUrlPermissionUpsertRequestDto;
import com.example.helloworld.entity.invest.auth.InvestPermission;
import com.example.helloworld.entity.invest.auth.InvestRole;
import com.example.helloworld.entity.invest.auth.InvestUrlPermission;
import com.example.helloworld.repository.invest.auth.InvestPermissionRepository;
import com.example.helloworld.repository.invest.auth.InvestRoleRepository;
import com.example.helloworld.repository.invest.auth.InvestUrlPermissionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional(transactionManager = "investTransactionManager")
public class InvestSystemUrlPermissionService {

    private static final List<String> HTTP_METHOD_OPTIONS = Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE");

    private final InvestUrlPermissionRepository investUrlPermissionRepository;
    private final InvestPermissionRepository investPermissionRepository;
    private final InvestRoleRepository investRoleRepository;

    public InvestSystemUrlPermissionService(
        InvestUrlPermissionRepository investUrlPermissionRepository,
        InvestPermissionRepository investPermissionRepository,
        InvestRoleRepository investRoleRepository
    ) {
        this.investUrlPermissionRepository = investUrlPermissionRepository;
        this.investPermissionRepository = investPermissionRepository;
        this.investRoleRepository = investRoleRepository;
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public Page<SystemUrlPermissionDto> getPaged(
        String urlPattern,
        String httpMethod,
        Boolean isPublic,
        String requiredPermission,
        Boolean isActive,
        int page,
        int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return investUrlPermissionRepository.findByFilters(
            normalize(urlPattern),
            normalizeMethod(httpMethod),
            isPublic,
            normalize(requiredPermission),
            isActive,
            pageable
        ).map(this::toDto);
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public SystemUrlPermissionDto getDetail(Long id) {
        return toDto(getUrlPermission(id));
    }

    public SystemUrlPermissionDto create(SystemUrlPermissionUpsertRequestDto request) {
        InvestUrlPermission urlPermission = new InvestUrlPermission();
        applyMutableFields(urlPermission, request, null);
        return toDto(investUrlPermissionRepository.save(urlPermission));
    }

    public SystemUrlPermissionDto update(Long id, SystemUrlPermissionUpsertRequestDto request) {
        InvestUrlPermission existing = getUrlPermission(id);
        applyMutableFields(existing, request, id);
        return toDto(investUrlPermissionRepository.save(existing));
    }

    public void delete(Long id) {
        getUrlPermission(id);
        investUrlPermissionRepository.deleteById(id);
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public List<SystemPermissionOptionDto> getPermissionOptions() {
        return investPermissionRepository.findAllByOrderByPermissionCodeAsc().stream()
            .map(this::toPermissionOptionDto)
            .toList();
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public List<SystemHttpMethodOptionDto> getHttpMethodOptions() {
        return HTTP_METHOD_OPTIONS.stream().map(method -> {
            SystemHttpMethodOptionDto dto = new SystemHttpMethodOptionDto();
            dto.setValue(method);
            dto.setLabel(method);
            return dto;
        }).toList();
    }

    private void applyMutableFields(InvestUrlPermission target, SystemUrlPermissionUpsertRequestDto request, Long currentId) {
        String urlPattern = require(request.getUrlPattern(), "urlPattern");
        String normalizedMethod = normalizeMethod(request.getHttpMethod());

        validateUniqueness(urlPattern, normalizedMethod, currentId);

        target.setUrlPattern(urlPattern);
        target.setHttpMethod(normalizedMethod);
        target.setIsPublic(Boolean.TRUE.equals(request.getIsPublic()));

        String requiredRole = normalize(request.getRequiredRole());
        if (requiredRole != null) {
            InvestRole role = investRoleRepository.findByRoleName(requiredRole)
                .orElseThrow(() -> new RuntimeException("requiredRole 不存在：" + requiredRole));
            target.setRequiredRole(role.getRoleName());
        } else {
            target.setRequiredRole(null);
        }

        String requiredPermission = normalize(request.getRequiredPermission());
        if (requiredPermission != null) {
            InvestPermission permission = investPermissionRepository.findByPermissionCodeIgnoreCase(requiredPermission)
                .orElseThrow(() -> new RuntimeException("requiredPermission 不存在：" + requiredPermission));
            target.setRequiredPermission(permission.getPermissionCode());
        } else {
            target.setRequiredPermission(null);
        }

        if (Boolean.TRUE.equals(target.getIsPublic())) {
            target.setRequiredRole(null);
            target.setRequiredPermission(null);
        }

        target.setDescription(normalize(request.getDescription()));
        target.setIsActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive());
        target.setOrderIndex(request.getOrderIndex() == null ? 0 : request.getOrderIndex());
    }

    private void validateUniqueness(String urlPattern, String httpMethod, Long currentId) {
        Optional<InvestUrlPermission> existing = investUrlPermissionRepository.findByUrlPatternAndHttpMethod(urlPattern, httpMethod);
        if (existing.isPresent() && (currentId == null || !Objects.equals(existing.get().getId(), currentId))) {
            String method = httpMethod == null ? "ALL" : httpMethod;
            throw new RuntimeException("URL 與方法組合已存在：" + urlPattern + " [" + method + "]");
        }
    }

    private InvestUrlPermission getUrlPermission(Long id) {
        return investUrlPermissionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("找不到 URL 權限，id=" + id));
    }

    private SystemUrlPermissionDto toDto(InvestUrlPermission permission) {
        SystemUrlPermissionDto dto = new SystemUrlPermissionDto();
        dto.setId(permission.getId());
        dto.setUrlPattern(permission.getUrlPattern());
        dto.setHttpMethod(permission.getHttpMethod());
        dto.setIsPublic(Boolean.TRUE.equals(permission.getIsPublic()));
        dto.setRequiredRole(permission.getRequiredRole());
        dto.setRequiredPermission(permission.getRequiredPermission());
        dto.setDescription(permission.getDescription());
        dto.setIsActive(Boolean.TRUE.equals(permission.getIsActive()));
        dto.setOrderIndex(permission.getOrderIndex());
        dto.setCreatedAt(permission.getCreatedAt());
        dto.setUpdatedAt(permission.getUpdatedAt());
        return dto;
    }

    private SystemPermissionOptionDto toPermissionOptionDto(InvestPermission permission) {
        SystemPermissionOptionDto dto = new SystemPermissionOptionDto();
        dto.setId(permission.getId());
        dto.setPermissionCode(permission.getPermissionCode());
        dto.setPermissionName(permission.getPermissionName());
        dto.setResource(permission.getResource());
        dto.setAction(permission.getAction());
        return dto;
    }

    private String normalizeMethod(String method) {
        String normalized = normalize(method);
        if (normalized == null) {
            return null;
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (!HTTP_METHOD_OPTIONS.contains(upper)) {
            throw new RuntimeException("不支援的 HTTP method：" + method);
        }
        return upper;
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
