package com.example.helloworld.controller.invest;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.dto.invest.SystemHttpMethodOptionDto;
import com.example.helloworld.dto.invest.SystemPermissionOptionDto;
import com.example.helloworld.dto.invest.SystemUrlPermissionDto;
import com.example.helloworld.dto.invest.SystemUrlPermissionUpsertRequestDto;
import com.example.helloworld.service.invest.system.InvestSystemUrlPermissionService;
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
@RequestMapping("/api/invest/system/url-permissions")
@CrossOrigin(origins = "*")
public class SystemUrlPermissionController {

    private final InvestSystemUrlPermissionService investSystemUrlPermissionService;

    public SystemUrlPermissionController(InvestSystemUrlPermissionService investSystemUrlPermissionService) {
        this.investSystemUrlPermissionService = investSystemUrlPermissionService;
    }

    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<PageResponse<SystemUrlPermissionDto>>> getPaged(
        @RequestParam(required = false) String urlPattern,
        @RequestParam(required = false) String httpMethod,
        @RequestParam(required = false) Boolean isPublic,
        @RequestParam(required = false) String requiredPermission,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        try {
            Page<SystemUrlPermissionDto> result = investSystemUrlPermissionService.getPaged(
                urlPattern,
                httpMethod,
                isPublic,
                requiredPermission,
                isActive,
                page,
                size
            );
            PageResponse<SystemUrlPermissionDto> response = new PageResponse<>(
                result.getContent(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
            );
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢 URL 權限列表失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/permission-options")
    public ResponseEntity<ApiResponse<List<SystemPermissionOptionDto>>> getPermissionOptions() {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investSystemUrlPermissionService.getPermissionOptions()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢權限選項失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/method-options")
    public ResponseEntity<ApiResponse<List<SystemHttpMethodOptionDto>>> getMethodOptions() {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investSystemUrlPermissionService.getHttpMethodOptions()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢 HTTP 方法選項失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<SystemUrlPermissionDto>> getDetail(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investSystemUrlPermissionService.getDetail(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢 URL 權限明細失敗：" + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SystemUrlPermissionDto>> create(@RequestBody SystemUrlPermissionUpsertRequestDto request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investSystemUrlPermissionService.create(request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("新增 URL 權限失敗：" + e.getMessage()));
        }
    }

    @PutMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<SystemUrlPermissionDto>> update(
        @PathVariable Long id,
        @RequestBody SystemUrlPermissionUpsertRequestDto request
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investSystemUrlPermissionService.update(id, request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新 URL 權限失敗：" + e.getMessage()));
        }
    }

    @DeleteMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            investSystemUrlPermissionService.delete(id);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("刪除 URL 權限失敗：" + e.getMessage()));
        }
    }
}
