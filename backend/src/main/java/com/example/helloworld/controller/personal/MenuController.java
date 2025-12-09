package com.example.helloworld.controller.personal;

import com.example.helloworld.entity.personal.MenuItem;
import com.example.helloworld.service.personal.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/menus")
@CrossOrigin(origins = "*")
public class MenuController {

    @Autowired
    private MenuService menuService;

    /**
     * 獲取當前用戶可見的菜單
     */
    @GetMapping
    public ResponseEntity<List<MenuService.MenuItemDTO>> getMenus() {
        List<MenuService.MenuItemDTO> menus = menuService.getVisibleMenus();
        return ResponseEntity.ok(menus);
    }

    /**
     * 獲取所有菜單項（管理用）
     */
    @GetMapping("/all")
    public ResponseEntity<List<MenuItem>> getAllMenuItems() {
        List<MenuItem> menuItems = menuService.getAllMenuItems();
        return ResponseEntity.ok(menuItems);
    }

    /**
     * 根據 ID 獲取菜單項
     */
    @GetMapping("/{id}")
    public ResponseEntity<MenuItem> getMenuItemById(@PathVariable Long id) {
        Optional<MenuItem> menuItem = menuService.getMenuItemById(id);
        return menuItem.map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 創建菜單項
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createMenuItem(@RequestBody MenuItem menuItem) {
        try {
            // 確保 parentId 類型正確（處理 Integer 到 Long 的轉換）
            if (menuItem.getParentId() != null) {
                Object parentIdObj = menuItem.getParentId();
                if (parentIdObj instanceof Integer) {
                    menuItem.setParentId(((Integer) parentIdObj).longValue());
                } else if (parentIdObj instanceof Number) {
                    menuItem.setParentId(((Number) parentIdObj).longValue());
                }
            }
            
            MenuItem created = menuService.createMenuItem(menuItem);
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
    public ResponseEntity<Map<String, Object>> updateMenuItem(@PathVariable Long id, @RequestBody MenuItem menuItem) {
        try {
            // 確保 parentId 類型正確（處理 Integer 到 Long 的轉換）
            if (menuItem.getParentId() != null) {
                Object parentIdObj = menuItem.getParentId();
                if (parentIdObj instanceof Integer) {
                    menuItem.setParentId(((Integer) parentIdObj).longValue());
                } else if (parentIdObj instanceof Number) {
                    menuItem.setParentId(((Number) parentIdObj).longValue());
                }
            }
            
            MenuItem updated = menuService.updateMenuItem(id, menuItem);
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
            menuService.deleteMenuItem(id);
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
