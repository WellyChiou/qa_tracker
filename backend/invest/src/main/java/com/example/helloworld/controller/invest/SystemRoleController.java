package com.example.helloworld.controller.invest;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.dto.invest.SystemPermissionOptionDto;
import com.example.helloworld.dto.invest.SystemRoleDto;
import com.example.helloworld.dto.invest.SystemRolePermissionsUpdateRequestDto;
import com.example.helloworld.dto.invest.SystemRoleUpsertRequestDto;
import com.example.helloworld.service.invest.system.InvestSystemRoleService;
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
@RequestMapping("/api/invest/system/roles")
@CrossOrigin(origins = "*")
public class SystemRoleController {

    private final InvestSystemRoleService investSystemRoleService;

    public SystemRoleController(InvestSystemRoleService investSystemRoleService) {
        this.investSystemRoleService = investSystemRoleService;
    }

    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<PageResponse<SystemRoleDto>>> getPaged(
        @RequestParam(required = false) String roleName,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        try {
            Page<SystemRoleDto> result = investSystemRoleService.getPaged(roleName, page, size);
            PageResponse<SystemRoleDto> response = new PageResponse<>(
                result.getContent(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
            );
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢角色列表失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/permission-options")
    public ResponseEntity<ApiResponse<List<SystemPermissionOptionDto>>> getPermissionOptions() {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investSystemRoleService.getPermissionOptions()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢權限清單失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<SystemRoleDto>> getDetail(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investSystemRoleService.getDetail(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢角色明細失敗：" + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SystemRoleDto>> create(@RequestBody SystemRoleUpsertRequestDto request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investSystemRoleService.create(request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("新增角色失敗：" + e.getMessage()));
        }
    }

    @PutMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<SystemRoleDto>> update(
        @PathVariable Long id,
        @RequestBody SystemRoleUpsertRequestDto request
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investSystemRoleService.update(id, request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新角色失敗：" + e.getMessage()));
        }
    }

    @DeleteMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            investSystemRoleService.delete(id);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("刪除角色失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/{id:[0-9]+}/permissions")
    public ResponseEntity<ApiResponse<List<SystemPermissionOptionDto>>> getRolePermissions(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investSystemRoleService.getRolePermissions(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢角色權限失敗：" + e.getMessage()));
        }
    }

    @PutMapping("/{id:[0-9]+}/permissions")
    public ResponseEntity<ApiResponse<List<SystemPermissionOptionDto>>> updateRolePermissions(
        @PathVariable Long id,
        @RequestBody SystemRolePermissionsUpdateRequestDto request
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investSystemRoleService.updateRolePermissions(id, request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新角色權限失敗：" + e.getMessage()));
        }
    }
}
