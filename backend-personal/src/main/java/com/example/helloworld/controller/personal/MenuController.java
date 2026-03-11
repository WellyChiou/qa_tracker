package com.example.helloworld.controller.personal;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.entity.personal.MenuItem;
import com.example.helloworld.service.personal.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/personal/menus")
@CrossOrigin(origins = "*")
public class MenuController {

    @Autowired
    private MenuService menuService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MenuService.MenuItemDTO>>> getMenus() {
        List<MenuService.MenuItemDTO> menus = menuService.getVisibleMenus();
        return ResponseEntity.ok(ApiResponse.ok(menus));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<MenuItem>>> getAllMenuItems() {
        List<MenuItem> menuItems = menuService.getAllMenuItems();
        return ResponseEntity.ok(ApiResponse.ok(menuItems));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MenuItem>> getMenuItemById(@PathVariable Long id) {
        Optional<MenuItem> menuItem = menuService.getMenuItemById(id);
        return menuItem.map(m -> ResponseEntity.ok(ApiResponse.ok(m)))
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("菜單項不存在")));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MenuItem>> createMenuItem(@RequestBody MenuItem menuItem) {
        try {
            if (menuItem.getParentId() != null) {
                Object parentIdObj = menuItem.getParentId();
                if (parentIdObj instanceof Integer) {
                    menuItem.setParentId(((Integer) parentIdObj).longValue());
                } else if (parentIdObj instanceof Number) {
                    menuItem.setParentId(((Number) parentIdObj).longValue());
                }
            }
            MenuItem created = menuService.createMenuItem(menuItem);
            return ResponseEntity.ok(ApiResponse.ok(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("創建失敗: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MenuItem>> updateMenuItem(@PathVariable Long id, @RequestBody MenuItem menuItem) {
        try {
            if (menuItem.getParentId() != null) {
                Object parentIdObj = menuItem.getParentId();
                if (parentIdObj instanceof Integer) {
                    menuItem.setParentId(((Integer) parentIdObj).longValue());
                } else if (parentIdObj instanceof Number) {
                    menuItem.setParentId(((Number) parentIdObj).longValue());
                }
            }
            MenuItem updated = menuService.updateMenuItem(id, menuItem);
            return ResponseEntity.ok(ApiResponse.ok(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新失敗: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMenuItem(@PathVariable Long id) {
        try {
            menuService.deleteMenuItem(id);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("刪除失敗: " + e.getMessage()));
        }
    }
}
