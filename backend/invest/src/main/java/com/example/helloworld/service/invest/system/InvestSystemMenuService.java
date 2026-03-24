package com.example.helloworld.service.invest.system;

import com.example.helloworld.dto.invest.SystemMenuDto;
import com.example.helloworld.dto.invest.SystemMenuOptionDto;
import com.example.helloworld.dto.invest.SystemMenuUpsertRequestDto;
import com.example.helloworld.dto.invest.SystemPermissionOptionDto;
import com.example.helloworld.entity.invest.auth.InvestMenuItem;
import com.example.helloworld.entity.invest.auth.InvestPermission;
import com.example.helloworld.repository.invest.auth.InvestMenuItemRepository;
import com.example.helloworld.repository.invest.auth.InvestPermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(transactionManager = "investTransactionManager")
public class InvestSystemMenuService {

    private final InvestMenuItemRepository investMenuItemRepository;
    private final InvestPermissionRepository investPermissionRepository;

    public InvestSystemMenuService(
        InvestMenuItemRepository investMenuItemRepository,
        InvestPermissionRepository investPermissionRepository
    ) {
        this.investMenuItemRepository = investMenuItemRepository;
        this.investPermissionRepository = investPermissionRepository;
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public List<SystemMenuDto> getTree() {
        return buildTree(investMenuItemRepository.findAllForSystemTree());
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public SystemMenuDto getDetail(Long id) {
        InvestMenuItem menu = getMenu(id);
        SystemMenuDto dto = toDto(menu);
        dto.setChildren(List.of());
        return dto;
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public List<SystemMenuOptionDto> getParentOptions(Long excludeMenuId) {
        List<InvestMenuItem> menus = investMenuItemRepository.findAllForSystemTree();
        Set<Long> excludedIds = new HashSet<>();

        if (excludeMenuId != null) {
            excludedIds.add(excludeMenuId);
            excludedIds.addAll(findDescendantIds(excludeMenuId, menus));
        }

        return menus.stream()
            .filter(menu -> !excludedIds.contains(menu.getId()))
            .map(this::toOptionDto)
            .toList();
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public List<SystemPermissionOptionDto> getRequiredPermissionOptions() {
        return investPermissionRepository.findAllByOrderByPermissionCodeAsc().stream()
            .map(this::toPermissionOptionDto)
            .toList();
    }

    public SystemMenuDto create(SystemMenuUpsertRequestDto request) {
        String menuCode = require(request.getMenuCode(), "menuCode");
        String menuName = require(request.getMenuName(), "menuName");
        ensureMenuCodeAvailable(menuCode, null);

        InvestMenuItem menu = new InvestMenuItem();
        menu.setMenuCode(menuCode);
        menu.setMenuName(menuName);
        applyMutableFields(menu, request, null);

        InvestMenuItem saved = investMenuItemRepository.save(menu);
        return getDetail(saved.getId());
    }

    public SystemMenuDto update(Long id, SystemMenuUpsertRequestDto request) {
        InvestMenuItem menu = getMenu(id);

        String menuCode = require(request.getMenuCode(), "menuCode");
        String menuName = require(request.getMenuName(), "menuName");
        ensureMenuCodeAvailable(menuCode, id);

        menu.setMenuCode(menuCode);
        menu.setMenuName(menuName);
        applyMutableFields(menu, request, id);

        investMenuItemRepository.save(menu);
        return getDetail(id);
    }

    public SystemMenuDto setEnabled(Long id, boolean enabled) {
        InvestMenuItem menu = getMenu(id);
        menu.setIsActive(enabled);
        investMenuItemRepository.save(menu);
        return getDetail(id);
    }

    public void delete(Long id) {
        getMenu(id);

        long childCount = investMenuItemRepository.countChildren(id);
        if (childCount > 0) {
            throw new RuntimeException("菜單下仍有子節點，請先調整子節點後再刪除");
        }

        investMenuItemRepository.deleteById(id);
    }

    private void applyMutableFields(InvestMenuItem menu, SystemMenuUpsertRequestDto request, Long selfId) {
        Long parentId = request.getParentId();
        validateParentRelation(selfId, parentId);

        menu.setParentId(parentId);
        menu.setIcon(normalize(request.getIcon()));
        menu.setUrl(normalize(request.getUrl()));
        menu.setOrderIndex(request.getOrderIndex() == null ? 0 : request.getOrderIndex());
        menu.setIsActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive());

        boolean isRootMenu = parentId == null;
        if (!isRootMenu) {
            menu.setShowInDashboard(false);
        } else {
            menu.setShowInDashboard(request.getShowInDashboard() == null ? Boolean.TRUE : request.getShowInDashboard());
        }

        String permissionCode = normalize(request.getRequiredPermission());
        if (permissionCode != null) {
            InvestPermission permission = investPermissionRepository.findByPermissionCodeIgnoreCase(permissionCode)
                .orElseThrow(() -> new RuntimeException("requiredPermission 不存在：" + permissionCode));
            menu.setRequiredPermission(permission.getPermissionCode());
        } else {
            menu.setRequiredPermission(null);
        }

        menu.setDescription(normalize(request.getDescription()));
    }

    private void validateParentRelation(Long selfId, Long parentId) {
        if (parentId == null) {
            return;
        }

        if (selfId != null && Objects.equals(selfId, parentId)) {
            throw new RuntimeException("父菜單不可設定為自己");
        }

        if (!investMenuItemRepository.existsById(parentId)) {
            throw new RuntimeException("指定的父菜單不存在，id=" + parentId);
        }

        if (selfId == null) {
            return;
        }

        List<InvestMenuItem> menus = investMenuItemRepository.findAllForSystemTree();
        Set<Long> descendantIds = findDescendantIds(selfId, menus);
        if (descendantIds.contains(parentId)) {
            throw new RuntimeException("不可將父菜單設定為自己的子節點");
        }
    }

    private Set<Long> findDescendantIds(Long rootId, List<InvestMenuItem> allMenus) {
        Map<Long, List<Long>> childrenMap = new HashMap<>();
        for (InvestMenuItem menu : allMenus) {
            if (menu.getParentId() != null) {
                childrenMap.computeIfAbsent(menu.getParentId(), key -> new ArrayList<>()).add(menu.getId());
            }
        }

        Set<Long> descendants = new HashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        queue.add(rootId);
        while (!queue.isEmpty()) {
            Long current = queue.removeFirst();
            List<Long> children = childrenMap.getOrDefault(current, List.of());
            for (Long childId : children) {
                if (descendants.add(childId)) {
                    queue.add(childId);
                }
            }
        }

        return descendants;
    }

    private List<SystemMenuDto> buildTree(List<InvestMenuItem> menus) {
        Map<Long, SystemMenuDto> nodeMap = new HashMap<>();
        for (InvestMenuItem menu : menus) {
            SystemMenuDto dto = toDto(menu);
            dto.setChildren(new ArrayList<>());
            nodeMap.put(menu.getId(), dto);
        }

        List<SystemMenuDto> roots = new ArrayList<>();
        for (InvestMenuItem menu : menus) {
            SystemMenuDto current = nodeMap.get(menu.getId());
            if (menu.getParentId() == null) {
                roots.add(current);
            } else {
                SystemMenuDto parent = nodeMap.get(menu.getParentId());
                if (parent != null) {
                    parent.getChildren().add(current);
                } else {
                    roots.add(current);
                }
            }
        }

        sortTree(roots);
        return roots;
    }

    private void sortTree(List<SystemMenuDto> nodes) {
        nodes.sort(Comparator
            .comparing(SystemMenuDto::getOrderIndex, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(SystemMenuDto::getId, Comparator.nullsLast(Long::compareTo)));

        for (SystemMenuDto node : nodes) {
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                sortTree(node.getChildren());
            }
        }
    }

    private InvestMenuItem getMenu(Long id) {
        return investMenuItemRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("找不到菜單，id=" + id));
    }

    private void ensureMenuCodeAvailable(String menuCode, Long currentId) {
        Optional<InvestMenuItem> existing = investMenuItemRepository.findByMenuCode(menuCode);
        if (existing.isPresent() && (currentId == null || !Objects.equals(existing.get().getId(), currentId))) {
            throw new RuntimeException("菜單代碼已存在：" + menuCode);
        }
    }

    private SystemMenuDto toDto(InvestMenuItem menu) {
        SystemMenuDto dto = new SystemMenuDto();
        dto.setId(menu.getId());
        dto.setMenuCode(menu.getMenuCode());
        dto.setMenuName(menu.getMenuName());
        dto.setIcon(menu.getIcon());
        dto.setUrl(menu.getUrl());
        dto.setParentId(menu.getParentId());
        dto.setOrderIndex(menu.getOrderIndex());
        dto.setIsActive(Boolean.TRUE.equals(menu.getIsActive()));
        dto.setShowInDashboard(Boolean.TRUE.equals(menu.getShowInDashboard()));
        dto.setRequiredPermission(menu.getRequiredPermission());
        dto.setDescription(menu.getDescription());
        dto.setCreatedAt(menu.getCreatedAt());
        dto.setUpdatedAt(menu.getUpdatedAt());
        return dto;
    }

    private SystemMenuOptionDto toOptionDto(InvestMenuItem menu) {
        SystemMenuOptionDto dto = new SystemMenuOptionDto();
        dto.setId(menu.getId());
        dto.setMenuCode(menu.getMenuCode());
        dto.setMenuName(menu.getMenuName());
        dto.setParentId(menu.getParentId());
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
