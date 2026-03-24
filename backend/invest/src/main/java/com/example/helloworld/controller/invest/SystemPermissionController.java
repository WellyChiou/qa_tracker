package com.example.helloworld.controller.invest;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.dto.invest.SystemPermissionDto;
import com.example.helloworld.dto.invest.SystemPermissionRoleBindingDto;
import com.example.helloworld.dto.invest.SystemPermissionUpsertRequestDto;
import com.example.helloworld.service.invest.system.InvestSystemPermissionService;
import org.springframework.data.domain.Page;
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
@RequestMapping("/api/invest/system/permissions")
@CrossOrigin(origins = "*")
public class SystemPermissionController {

    private final InvestSystemPermissionService investSystemPermissionService;

    public SystemPermissionController(InvestSystemPermissionService investSystemPermissionService) {
        this.investSystemPermissionService = investSystemPermissionService;
    }

    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<PageResponse<SystemPermissionDto>>> getPaged(
        @RequestParam(required = false) String permissionCode,
        @RequestParam(required = false) String resource,
        @RequestParam(required = false) String action,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        try {
            Page<SystemPermissionDto> result = investSystemPermissionService.getPaged(permissionCode, resource, action, page, size);
            PageResponse<SystemPermissionDto> response = new PageResponse<>(
                result.getContent(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
            );
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢權限列表失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<SystemPermissionDto>> getDetail(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investSystemPermissionService.getDetail(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢權限明細失敗：" + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SystemPermissionDto>> create(@RequestBody SystemPermissionUpsertRequestDto request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investSystemPermissionService.create(request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("新增權限失敗：" + e.getMessage()));
        }
    }

    @PutMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<SystemPermissionDto>> update(
        @PathVariable Long id,
        @RequestBody SystemPermissionUpsertRequestDto request
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investSystemPermissionService.update(id, request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新權限失敗：" + e.getMessage()));
        }
    }

    @DeleteMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            investSystemPermissionService.delete(id);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("刪除權限失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/{id:[0-9]+}/role-bindings")
    public ResponseEntity<ApiResponse<List<SystemPermissionRoleBindingDto>>> getRoleBindings(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investSystemPermissionService.getRoleBindings(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢權限角色綁定失敗：" + e.getMessage()));
        }
    }
}
