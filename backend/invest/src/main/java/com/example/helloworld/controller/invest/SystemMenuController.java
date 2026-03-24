package com.example.helloworld.controller.invest;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.invest.SystemMenuDto;
import com.example.helloworld.dto.invest.SystemMenuEnabledRequestDto;
import com.example.helloworld.dto.invest.SystemMenuOptionDto;
import com.example.helloworld.dto.invest.SystemMenuUpsertRequestDto;
import com.example.helloworld.dto.invest.SystemPermissionOptionDto;
import com.example.helloworld.service.invest.system.InvestSystemMenuService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/invest/system/menus")
@CrossOrigin(origins = "*")
public class SystemMenuController {

    private final InvestSystemMenuService investSystemMenuService;

    public SystemMenuController(InvestSystemMenuService investSystemMenuService) {
        this.investSystemMenuService = investSystemMenuService;
    }

    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<SystemMenuDto>>> getTree() {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investSystemMenuService.getTree()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢菜單樹失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/parent-options")
    public ResponseEntity<ApiResponse<List<SystemMenuOptionDto>>> getParentOptions(
        @RequestParam(required = false) Long excludeId
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investSystemMenuService.getParentOptions(excludeId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢父菜單選項失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/required-permission-options")
    public ResponseEntity<ApiResponse<List<SystemPermissionOptionDto>>> getRequiredPermissionOptions() {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investSystemMenuService.getRequiredPermissionOptions()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢所需權限選項失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<SystemMenuDto>> getDetail(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investSystemMenuService.getDetail(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢菜單明細失敗：" + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SystemMenuDto>> create(@RequestBody SystemMenuUpsertRequestDto request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investSystemMenuService.create(request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("新增菜單失敗：" + e.getMessage()));
        }
    }

    @PutMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<SystemMenuDto>> update(
        @PathVariable Long id,
        @RequestBody SystemMenuUpsertRequestDto request
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investSystemMenuService.update(id, request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新菜單失敗：" + e.getMessage()));
        }
    }

    @PutMapping("/{id:[0-9]+}/enabled")
    public ResponseEntity<ApiResponse<SystemMenuDto>> setEnabled(
        @PathVariable Long id,
        @RequestBody SystemMenuEnabledRequestDto request
    ) {
        try {
            if (request == null || request.getEnabled() == null) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("enabled 為必填"));
            }
            return ResponseEntity.ok(ApiResponse.ok(investSystemMenuService.setEnabled(id, request.getEnabled())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新菜單啟用狀態失敗：" + e.getMessage()));
        }
    }

    @DeleteMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            investSystemMenuService.delete(id);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("刪除菜單失敗：" + e.getMessage()));
        }
    }
}
