package com.example.helloworld.controller.church;

import com.example.helloworld.entity.church.ChurchMenuItem;
import com.example.helloworld.service.church.ChurchMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/church/menus")
@CrossOrigin(origins = "*")
public class ChurchMenuController {

    @Autowired
    private ChurchMenuService churchMenuService;

    /**
     * 獲取前台菜單（公開訪問）
     */
    @GetMapping("/frontend")
    public ResponseEntity<List<ChurchMenuService.MenuItemDTO>> getFrontendMenus() {
        List<ChurchMenuService.MenuItemDTO> menus = churchMenuService.getFrontendMenus();
        return ResponseEntity.ok(menus);
    }

    /**
     * 獲取後台菜單（需要登入）
     */
    @GetMapping("/admin")
    public ResponseEntity<List<ChurchMenuService.MenuItemDTO>> getAdminMenus() {
        List<ChurchMenuService.MenuItemDTO> menus = churchMenuService.getAdminMenus();
        return ResponseEntity.ok(menus);
    }

    /**
     * 獲取儀表板快速操作菜單（需要登入）
     */
    @GetMapping("/dashboard")
    public ResponseEntity<List<ChurchMenuService.MenuItemDTO>> getDashboardQuickActions() {
        List<ChurchMenuService.MenuItemDTO> menus = churchMenuService.getDashboardQuickActions();
        return ResponseEntity.ok(menus);
    }

    /**
     * 獲取所有菜單項（管理用，需要管理權限，支持分頁和過濾）
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllMenuItems(
            @RequestParam(required = false) String menuCode,
            @RequestParam(required = false) String menuName,
            @RequestParam(required = false) String menuType,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<ChurchMenuItem> menusPage = churchMenuService.getAllMenuItems(menuCode, menuName, menuType, isActive, page, size);
            Map<String, Object> response = new HashMap<>();
            response.put("content", menusPage.getContent());
            response.put("totalElements", menusPage.getTotalElements());
            response.put("totalPages", menusPage.getTotalPages());
            response.put("currentPage", menusPage.getNumber());
            response.put("size", menusPage.getSize());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "獲取菜單列表失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 根據 ID 獲取菜單項
     */
    @GetMapping("/{id}")
    public ResponseEntity<ChurchMenuItem> getMenuItemById(@PathVariable Long id) {
        return churchMenuService.getMenuItemById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 創建菜單項
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createMenuItem(@RequestBody ChurchMenuItem menuItem) {
        try {
            ChurchMenuItem created = churchMenuService.createMenuItem(menuItem);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "菜單項創建成功");
            response.put("data", created);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "創建失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 更新菜單項
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateMenuItem(@PathVariable Long id, @RequestBody ChurchMenuItem menuItem) {
        try {
            ChurchMenuItem updated = churchMenuService.updateMenuItem(id, menuItem);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "菜單項更新成功");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 刪除菜單項
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteMenuItem(@PathVariable Long id) {
        try {
            churchMenuService.deleteMenuItem(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "菜單項刪除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "刪除失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

