package com.example.helloworld.controller.church;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
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
    public ResponseEntity<ApiResponse<PageResponse<ChurchMenuItem>>> getAllMenuItems(
            @RequestParam(required = false) String menuCode,
            @RequestParam(required = false) String menuName,
            @RequestParam(required = false) String menuType,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<ChurchMenuItem> menusPage = churchMenuService.getAllMenuItems(menuCode, menuName, menuType, isActive, page, size);
            PageResponse<ChurchMenuItem> pageResponse = new PageResponse<>(
                menusPage.getContent(),
                menusPage.getTotalElements(),
                menusPage.getTotalPages(),
                menusPage.getNumber(),
                menusPage.getSize()
            );
            return ResponseEntity.ok(ApiResponse.ok(pageResponse));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取菜單列表失敗：" + e.getMessage()));
        }
    }

    /**
     * 根據 ID 獲取菜單項
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ChurchMenuItem>> getMenuItemById(@PathVariable Long id) {
        return churchMenuService.getMenuItemById(id)
            .map(menuItem -> ResponseEntity.ok(ApiResponse.ok(menuItem)))
            .orElse(ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(ApiResponse.fail("找不到指定的菜單項")));
    }

    /**
     * 創建菜單項
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ChurchMenuItem>> createMenuItem(@RequestBody ChurchMenuItem menuItem) {
        try {
            ChurchMenuItem created = churchMenuService.createMenuItem(menuItem);
            return ResponseEntity.ok(ApiResponse.ok(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("創建失敗: " + e.getMessage()));
        }
    }

    /**
     * 更新菜單項
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ChurchMenuItem>> updateMenuItem(@PathVariable Long id, @RequestBody ChurchMenuItem menuItem) {
        try {
            ChurchMenuItem updated = churchMenuService.updateMenuItem(id, menuItem);
            return ResponseEntity.ok(ApiResponse.ok(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新失敗: " + e.getMessage()));
        }
    }

    /**
     * 刪除菜單項
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMenuItem(@PathVariable Long id) {
        try {
            churchMenuService.deleteMenuItem(id);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("刪除失敗: " + e.getMessage()));
        }
    }
}

