package com.example.helloworld.service.invest.system;

import com.example.helloworld.dto.invest.SystemUserDto;
import com.example.helloworld.dto.invest.SystemUserUpsertRequestDto;
import com.example.helloworld.entity.invest.auth.InvestPermission;
import com.example.helloworld.entity.invest.auth.InvestRole;
import com.example.helloworld.entity.invest.auth.InvestUser;
import com.example.helloworld.repository.invest.auth.InvestUserRepository;
import com.example.helloworld.service.invest.auth.InvestCurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(transactionManager = "investTransactionManager")
public class InvestSystemUserService {

    private final InvestUserRepository investUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final InvestCurrentUserService investCurrentUserService;

    public InvestSystemUserService(
        InvestUserRepository investUserRepository,
        PasswordEncoder passwordEncoder,
        InvestCurrentUserService investCurrentUserService
    ) {
        this.investUserRepository = investUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.investCurrentUserService = investCurrentUserService;
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public Page<SystemUserDto> getPaged(String username, String email, Boolean isEnabled, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return investUserRepository.findByFilters(normalize(username), normalize(email), isEnabled, pageable)
            .map(this::toDto);
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public SystemUserDto getDetail(String uid) {
        InvestUser user = investUserRepository.findByUidWithRolesAndPermissions(uid)
            .orElseThrow(() -> new RuntimeException("找不到用戶，uid=" + uid));
        return toDto(user);
    }

    public SystemUserDto create(SystemUserUpsertRequestDto request) {
        String username = require(request.getUsername(), "username");
        String email = require(request.getEmail(), "email");
        String password = require(request.getPassword(), "password");

        validatePassword(password);
        ensureUsernameAvailable(username, null);
        ensureEmailAvailable(email, null);

        InvestUser user = new InvestUser();
        user.setUid(UUID.randomUUID().toString());
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setDisplayName(normalize(request.getDisplayName()));
        user.setPhotoUrl(normalize(request.getPhotoUrl()));
        user.setProviderId("local");
        user.setIsEnabled(request.getIsEnabled() == null ? Boolean.TRUE : request.getIsEnabled());
        user.setIsAccountNonLocked(request.getIsAccountNonLocked() == null ? Boolean.TRUE : request.getIsAccountNonLocked());

        InvestUser saved = investUserRepository.save(user);
        return getDetail(saved.getUid());
    }

    public SystemUserDto update(String uid, SystemUserUpsertRequestDto request) {
        InvestUser user = investUserRepository.findByUid(uid)
            .orElseThrow(() -> new RuntimeException("找不到用戶，uid=" + uid));

        String username = require(request.getUsername(), "username");
        String email = require(request.getEmail(), "email");
        ensureUsernameAvailable(username, uid);
        ensureEmailAvailable(email, uid);

        user.setUsername(username);
        user.setEmail(email);
        user.setDisplayName(normalize(request.getDisplayName()));
        user.setPhotoUrl(normalize(request.getPhotoUrl()));
        if (request.getIsEnabled() != null) {
            validateSelfDisable(uid, request.getIsEnabled());
            user.setIsEnabled(request.getIsEnabled());
        }
        if (request.getIsAccountNonLocked() != null) {
            user.setIsAccountNonLocked(request.getIsAccountNonLocked());
        }

        investUserRepository.save(user);
        return getDetail(uid);
    }

    public SystemUserDto setEnabled(String uid, boolean enabled) {
        InvestUser user = investUserRepository.findByUid(uid)
            .orElseThrow(() -> new RuntimeException("找不到用戶，uid=" + uid));

        validateSelfDisable(uid, enabled);
        user.setIsEnabled(enabled);
        investUserRepository.save(user);
        return getDetail(uid);
    }

    public void resetPassword(String uid, String newPassword) {
        InvestUser user = investUserRepository.findByUid(uid)
            .orElseThrow(() -> new RuntimeException("找不到用戶，uid=" + uid));

        String normalizedPassword = require(newPassword, "newPassword");
        validatePassword(normalizedPassword);
        user.setPassword(passwordEncoder.encode(normalizedPassword));
        investUserRepository.save(user);
    }

    private void validateSelfDisable(String uid, boolean enabled) {
        if (enabled) {
            return;
        }
        String currentUserUid = investCurrentUserService.resolveCurrentUserUid();
        if (Objects.equals(currentUserUid, uid)) {
            throw new RuntimeException("不可停用目前登入中的帳號");
        }
    }

    private void ensureUsernameAvailable(String username, String currentUid) {
        investUserRepository.findByUsername(username).ifPresent(existing -> {
            if (currentUid == null || !Objects.equals(existing.getUid(), currentUid)) {
                throw new RuntimeException("用戶名已存在：" + username);
            }
        });
    }

    private void ensureEmailAvailable(String email, String currentUid) {
        investUserRepository.findByEmail(email).ifPresent(existing -> {
            if (currentUid == null || !Objects.equals(existing.getUid(), currentUid)) {
                throw new RuntimeException("電子郵件已存在：" + email);
            }
        });
    }

    private void validatePassword(String password) {
        if (password.length() < 6) {
            throw new RuntimeException("密碼至少需 6 碼");
        }
    }

    private SystemUserDto toDto(InvestUser user) {
        SystemUserDto dto = new SystemUserDto();
        dto.setUid(user.getUid());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setDisplayName(user.getDisplayName());
        dto.setPhotoUrl(user.getPhotoUrl());
        dto.setProviderId(user.getProviderId());
        dto.setIsEnabled(Boolean.TRUE.equals(user.getIsEnabled()));
        dto.setIsAccountNonLocked(Boolean.TRUE.equals(user.getIsAccountNonLocked()));
        dto.setLastLoginAt(user.getLastLoginAt());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());

        dto.setRoleNames(user.getRoles().stream()
            .map(InvestRole::getRoleName)
            .filter(Objects::nonNull)
            .sorted()
            .toList());

        Set<String> permissionCodes = new LinkedHashSet<>();
        user.getRoles().stream()
            .map(InvestRole::getPermissions)
            .filter(Objects::nonNull)
            .flatMap(Set::stream)
            .map(InvestPermission::getPermissionCode)
            .filter(Objects::nonNull)
            .forEach(permissionCodes::add);

        user.getPermissions().stream()
            .map(InvestPermission::getPermissionCode)
            .filter(Objects::nonNull)
            .sorted(Comparator.naturalOrder())
            .forEach(permissionCodes::add);

        dto.setPermissionCodes(permissionCodes.stream().toList());
        return dto;
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
